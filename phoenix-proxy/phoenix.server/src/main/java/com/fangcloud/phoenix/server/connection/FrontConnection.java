package com.fangcloud.phoenix.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.protocol.ProtocolParser;
import com.fangcloud.phoenix.protocol.ProtocolParser.Header;
import com.fangcloud.phoenix.server.io.NIOReactorPool;

public class FrontConnection extends AbstractConnection {
    private static final Logger LOGGER      = LoggerFactory.getLogger(FrontConnection.class);
    private Header              header;
    private byte[]              writeBuffer;
    private volatile long       createTime;
    private Stack<Byte>         eofStack    = new Stack<Byte>();
    private ProviderConnection  providerConnection;
    private volatile boolean    isHeaderEof = false;

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    @Override
    public void close() {
        super.close();
        if (providerConnection != null) {
            ProviderConnectionPool.getPoolInstance().returnConnection(providerConnection);
        }
    }

    public FrontConnection(SocketChannel channel, ProtocolParser protocolParser,
                           NIOReactorPool reactorPool) {
        super(channel, protocolParser);
    }

    @Override
    protected void doRead(SocketChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4096);
        buf.clear();
        int size = 0;
        try {
            while ((size = channel.read(buf)) > 0) {
                buf.flip();
                buf.mark();
                byte[] readBuf = new byte[size];
                buf.get(readBuf, buf.position(), buf.limit() - buf.position());
                if (writeBuffer == null) {
                    writeBuffer = readBuf;
                } else if (!isHeaderEof) {
                    writeBuffer = extendBfa(writeBuffer, readBuf);
                }
                if (headerEof(readBuf)) {
                    isHeaderEof = true;
                    header = protocolParser.parseHeader(writeBuffer);
                }

                buf.reset();
                if (providerConnection == null) {
                    providerConnection = ProviderConnectionPool.getPoolInstance()
                            .borrowConnection(header.getServiceName());
                }
                providerConnection.setFrontConnection(this);
                providerConnection.getChannel().write(buf);
                buf.clear();
            }

            if (size < 0) {
                LOGGER.debug("is close ???? fornt read size < 0! port:"
                        + this.getChannel().getRemoteAddress().toString());
                close();
                return;
            }
        } catch (Exception ex) {
            LOGGER.error("front read error,port:" + this.getChannel().getRemoteAddress().toString(),
                    ex);
            close();
        }

    }

    private boolean headerEof(byte[] readBuf) {
        for (int i = 0; i < readBuf.length; i++) {
            byte b = readBuf[i];
            if (b == '\r') {
                eofStack.push(b);
            } else if (b == '\n' && eofStack.size() >= 1 && eofStack.peek() == '\r') {
                eofStack.push(b);
            } else {
                if (!eofStack.isEmpty()) {
                    eofStack.clear();
                }
            }

            if (eofStack.size() == 4) {
                eofStack.clear();
                return true;
            }
        }
        return false;
    }

    private byte[] extendBfa(byte[] bfa, byte[] extendBfa) throws IOException {
        byte[] new_bfa = new byte[bfa.length + extendBfa.length];
        System.arraycopy(bfa, 0, new_bfa, 0, bfa.length);
        System.arraycopy(extendBfa, 0, new_bfa, bfa.length, extendBfa.length);
        return new_bfa;
    }

    @Override
    protected void doWrite(SocketChannel channel) {
        // nothing

    }

}

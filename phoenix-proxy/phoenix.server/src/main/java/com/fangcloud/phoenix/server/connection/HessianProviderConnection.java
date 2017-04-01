package com.fangcloud.phoenix.server.connection;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.protocol.ProtocolParser;

/**
 * 与Hessian服务提供者之间的连接
 * 
 * @author chenke
 * @date 2017年3月22日 下午1:24:36
 */
public class HessianProviderConnection extends AbstractConnection implements ProviderConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(HessianProviderConnection.class);
    private String              providerAddress;
    private int                 providerPort;
    private FrontConnection     fconn;
    //private Stack<Byte>         eofStack = new Stack<Byte>();

    public void close() {
        super.close();
    }

    public HessianProviderConnection(String providerAddress, int providerPort,
                                     SocketChannel channel, ProtocolParser protocolParser) {
        super(channel, protocolParser);
        this.providerAddress = providerAddress;
        this.providerPort = providerPort;
    }

    @Override
    protected void doRead(SocketChannel channel) {
        try {
            int size = 0;
            if (fconn != null && !fconn.isClosed()) {
                ByteBuffer buf = ByteBuffer.allocate(4096);
                buf.clear();

                while ((size = channel.read(buf)) > 0) {
                    buf.flip();
                    fconn.getChannel().write(buf);
                    buf.clear();
                }

                if (size < 0) {
                    close();
                    if (fconn != null) {
                        fconn.close();
                        LOGGER.debug("close pconn && fconn!");
                    } else {
                        ProviderConnectionPool.getPoolInstance().returnConnection(this);
                        LOGGER.debug("close pconn");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("read data error:", e);
        }
    }

    @Override
    protected void doWrite(SocketChannel channel) {
        //nothing

    }

    @Override
    public String getAddress() {
        return this.providerAddress;
    }

    @Override
    public int getPort() {
        return this.providerPort;
    }

    @Override
    public void setFrontConnection(FrontConnection fconn) {
        this.fconn = fconn;
    }

    @Override
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

}

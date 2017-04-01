package com.fangcloud.phoenix.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.protocol.ProtocolParser;

public abstract class AbstractConnection implements NIOConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnection.class);
    private SocketChannel       channel;
    private AtomicBoolean       isClosed;
    protected ProtocolParser    protocolParser;
    protected Selector          selector;
    protected SelectionKey      selectionKey;

    public AbstractConnection(SocketChannel channel, ProtocolParser protocolParser) {
        this.channel = channel;
        this.isClosed = new AtomicBoolean(false);
        this.protocolParser = protocolParser;
    }

    @Override
    public SelectionKey register(Selector selector) throws IOException {
        this.selector = selector;
        channel.configureBlocking(false);
        long start = System.currentTimeMillis();
        this.selectionKey = channel.register(this.selector, SelectionKey.OP_READ, this);
        LOGGER.debug("register time:" + (System.currentTimeMillis() - start));
        return selectionKey;
    }

    @Override
    public void close() {
        if (!isClosed.get()) {
            isClosed.set(true);
            try {
                LOGGER.debug("channel is close:" + channel.getRemoteAddress().toString());
                channel.close();
            } catch (Exception e) {
                LOGGER.error("close channel error:", e);
            }
        }
    }

    @Override
    public void asynRead() {
        try {
            long start = System.currentTimeMillis();
            doRead(channel);
            LOGGER.debug("read time:" + (System.currentTimeMillis() - start));
        } catch (Exception e) {
            LOGGER.error("read channel error:", e);
        }
    }

    protected abstract void doRead(SocketChannel channel) throws IOException;

    @Override
    public void asyncWrite() {
        long start = System.currentTimeMillis();
        try {
            doWrite(channel);
            LOGGER.debug("write time:" + (System.currentTimeMillis() - start) + ",class:"
                    + this.getClass().getName());
        } catch (Exception e) {
            LOGGER.error("write channel error:", e);
        }
    }

    protected abstract void doWrite(SocketChannel channel);

    @Override
    public SocketChannel getChannel() {
        return this.channel;
    }

    @Override
    public boolean isClosed() {
        return isClosed.get() && !channel.isConnected();
    }

}

package com.fangcloud.phoenix.server.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fangcloud.phoenix.server.connection.FrontConnection;
import com.fangcloud.phoenix.server.connection.FrontConnectionFactory;

/**
 * 连接accptor器
 * 
 * @author chenke
 * @date 2017年3月22日 下午5:20:05
 */
public final class NIOAcceptor extends Thread implements SocketAcceptor {
    private static final Logger       LOGGER = Logger.getLogger(NIOAcceptor.class);
    private final int                 port;
    private final Selector            selector;
    private final ServerSocketChannel serverChannel;
    private NIOReactorPool            reactorPool;

    public NIOAcceptor(String name, String bindIp, int port, NIOReactorPool reactorPool)
            throws IOException {
        super.setName(name);
        this.port = port;
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        /** 设置TCP属性 */
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 16 * 2);
        // backlog=100
        serverChannel.bind(new InetSocketAddress(bindIp, port), 100);
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.reactorPool = reactorPool;
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
        final Selector tSelector = this.selector;
        for (;;) {

            try {
                tSelector.select(1000);
                Set<SelectionKey> keys = tSelector.selectedKeys();
                try {
                    for (SelectionKey key : keys) {
                        if (key.isValid() && key.isAcceptable()) {
                            accept();
                        } else {
                            key.cancel();
                        }
                    }
                } finally {
                    keys.clear();
                }
            } catch (Exception e) {
                LOGGER.warn(getName(), e);
            }
        }
    }

    private void accept() {
        SocketChannel channel = null;
        try {
            channel = serverChannel.accept();
            channel.configureBlocking(false);
            FrontConnection fconn = FrontConnectionFactory.make(channel, reactorPool);
            NIOReactor reactor = reactorPool.getNextReactor();
            reactor.postRegister(fconn);
        } catch (Exception e) {
            LOGGER.warn(getName(), e);
            closeChannel(channel);
        }
    }

    private static void closeChannel(SocketChannel channel) {
        if (channel == null) {
            return;
        }
        Socket socket = channel.socket();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("closeChannelError", e);
            }
        }
        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.error("closeChannelError", e);
        }
    }

}

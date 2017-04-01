package com.fangcloud.phoenix.server.io;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.server.connection.NIOConnection;

/**
 * 网络事件反应器
 * 
 * @author chenke
 * @date 2017年3月22日 下午4:23:21
 */
public final class NIOReactor implements Runnable {
    private static final Logger                        LOGGER        = LoggerFactory
            .getLogger(NIOReactor.class);
    private final String                               name;
    private final Selector                             selector;
    private final ConcurrentLinkedQueue<NIOConnection> registerQueue = new ConcurrentLinkedQueue<NIOConnection>();

    public NIOReactor(String name) throws IOException {
        this.name = name;
        this.selector = Selector.open();
    }

    final void startup() {
        new Thread(this, name).start();
    }

    @Override
    public void run() {
        final Selector selector = this.selector;
        Set<SelectionKey> keys = null;
        for (;;) {
            try {
                selector.select(500);
                register(selector);
                keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    NIOConnection con = null;
                    try {
                        Object att = key.attachment();
                        if (att != null) {
                            con = (NIOConnection) att;
                            if (key.isValid() && key.isReadable()) {
                                try {
                                    con.asynRead();
                                } catch (Exception e) {
                                    LOGGER.warn("caught err: ", e);
                                    con.close();
                                    continue;
                                }
                            }
                            if (key.isValid() && key.isWritable()) {
                                con.asyncWrite();
                            }
                        } else {
                            key.cancel();
                        }
                    } catch (CancelledKeyException e) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(con + " socket key canceled");
                        }
                    } catch (Exception e) {
                        LOGGER.warn(con + " " + e);
                    } catch (final Throwable e) {
                        // Catch exceptions such as OOM and close connection if exists
                        //so that the reactor can keep running!
                        if (con != null) {
                            con.close();
                        }
                        LOGGER.error("caught err: ", e);
                        continue;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(name, e);
            } catch (final Throwable e) {
                // Catch exceptions such as OOM so that the reactor can keep running!
                LOGGER.error("caught err: ", e);
            } finally {
                if (keys != null) {
                    keys.clear();
                }
            }
        }

    }

    private void register(Selector selector) {
        NIOConnection c = null;
        if (registerQueue.isEmpty()) {
            return;
        }
        while ((c = registerQueue.poll()) != null) {
            try {
                c.register(selector);
            } catch (Exception e) {
                LOGGER.error("register nio connection failed:", e);
                c.close();
            }
        }
    }

    public final void postRegister(NIOConnection c) {
        registerQueue.offer(c);
        selector.wakeup();
    }

}

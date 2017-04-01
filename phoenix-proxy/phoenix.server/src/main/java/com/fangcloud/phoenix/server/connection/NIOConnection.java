package com.fangcloud.phoenix.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * NIO连接
 * 
 * @author chenke
 * @date 2017年3月22日 下午4:34:07
 */
public interface NIOConnection {

    public SelectionKey register(Selector selector) throws IOException;

    /**
     * 关闭连接
     */
    public void close();

    /**
     * 响应读取事件
     */
    public void asynRead();

    /**
     * 响应写入事件
     */
    public void asyncWrite();

    /**
     * 连接是否已经关闭
     * 
     * @return
     */
    public boolean isClosed();

    /**
     * 获取SocketChannel
     * 
     * @return
     */
    public SocketChannel getChannel();
}

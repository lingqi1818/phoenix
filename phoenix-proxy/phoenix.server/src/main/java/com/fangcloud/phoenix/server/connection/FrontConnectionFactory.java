package com.fangcloud.phoenix.server.connection;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;

import com.fangcloud.phoenix.protocol.HttpProtocolParser;
import com.fangcloud.phoenix.server.io.NIOReactorPool;

/**
 * 前端连接工厂
 * 
 * @author chenke
 * @date 2017年3月22日 下午5:40:26
 */
public abstract class FrontConnectionFactory {

    public static FrontConnection make(SocketChannel channel, NIOReactorPool reactorPool)
            throws IOException {
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        FrontConnection c = new FrontConnection(channel, new HttpProtocolParser(), reactorPool);
        c.setCreateTime(System.currentTimeMillis());
        return c;
    }

}

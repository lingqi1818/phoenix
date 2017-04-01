package com.fangcloud.phoenix.server.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.ServerException;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.common.protocol.Protocol;
import com.fangcloud.phoenix.server.io.NIOReactorPool;

/**
 * 服务提供者连接创建工厂
 * 
 * @author chenke
 * @date 2017年3月21日 下午5:39:07
 */
public class ProviderConnectionFactory implements PooledObjectFactory<ProviderConnection> {
    private String              providerAddress;
    private int                 providerPort;
    private Protocol            protocol;
    private NIOReactorPool      reactorPool;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderConnectionFactory.class);

    public ProviderConnectionFactory(String providerAddress, int providerPort, Protocol protocol,
                                     NIOReactorPool reactorPool) {
        this.providerAddress = providerAddress;
        this.providerPort = providerPort;
        this.protocol = protocol;
        this.reactorPool = reactorPool;
    }

    @Override
    public PooledObject<ProviderConnection> makeObject() throws Exception {
        LOGGER.debug("create pconn!!!");
        SocketChannel channel = makeSocketChannel();
        if (this.protocol == Protocol.HESSIAN) {
            HessianProviderConnection hconn = new HessianProviderConnection(providerAddress,
                    providerPort, channel, null);
            reactorPool.getNextReactor().postRegister(hconn);
            return new DefaultPooledObject<ProviderConnection>(hconn);
        }

        return null;
    }

    private SocketChannel makeSocketChannel() throws ServerException {
        try {
            SocketChannel socketChannel = SocketChannel
                    .open(new InetSocketAddress(providerAddress, providerPort));
            socketChannel.configureBlocking(false);
            return socketChannel;
        } catch (IOException e) {
            throw new ServerException("make socket channel error!!!", e);
        }
    }

    @Override
    public void destroyObject(PooledObject<ProviderConnection> p) throws Exception {
        ProviderConnection conn = p.getObject();
        if (conn != null) {
            conn.close();
            LOGGER.debug("destory pconn !");
        }

    }

    @Override
    public boolean validateObject(PooledObject<ProviderConnection> p) {
        NIOConnection c = p.getObject();
        return c.getChannel().isConnected();
    }

    @Override
    public void activateObject(PooledObject<ProviderConnection> p) throws Exception {
        //nothing

    }

    @Override
    public void passivateObject(PooledObject<ProviderConnection> p) throws Exception {
        //nothing

    }

}

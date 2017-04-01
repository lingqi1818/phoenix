package com.fangcloud.phoenix.server;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.server.buffer.BufferPool;
import com.fangcloud.phoenix.server.connection.ProviderConnectionPool;
import com.fangcloud.phoenix.server.io.NIOAcceptor;
import com.fangcloud.phoenix.server.io.NIOReactorPool;
import com.fangcloud.phoenix.server.io.SocketAcceptor;

/**
 * 默认实现，基于原生NIO和commons-pool，性能较高
 * 
 * @author chenke
 * @date 2017年4月1日 下午2:33:08
 */
public class DefaultPhoenixServer implements PhoenixServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderConnectionPool.class);
    private List<String>        serviceList;
    private String              ip;
    private int                 port;
    private String              registerAddr;
    private int                 reactors;

    @Override
    public void start() {
        try {
            if (serviceList == null || StringUtils.isEmpty(ip) || port <= 0
                    || StringUtils.isEmpty(registerAddr) || reactors <= 0) {
                LOGGER.error("Phoenix-server config has error,service List:" + serviceList + ",ip:"
                        + ip + ",port:" + port + ",regiserAddr:" + registerAddr + ",reactors:"
                        + reactors);
                return;
            }
            NIOReactorPool reactorPool = new NIOReactorPool(
                    BufferPool.LOCAL_BUF_THREAD_PREX + "Phoenix-Reactor", reactors);
            ProviderConnectionPool.makePool(registerAddr, serviceList, reactorPool);
            SocketAcceptor acceptor = new NIOAcceptor("Phoenix-server", ip, port, reactorPool);
            acceptor.start();
            LOGGER.info("start Phoenix-server success !!!");
        } catch (IOException e) {
            LOGGER.error("start Phoenix-server failed:", e);
        }
    }

    @Override
    public void stop() {
        //nothing
    }

    @Override
    public void setServices(List<String> serviceList) {
        this.serviceList = serviceList;

    }

    @Override
    public void setRegisterAddr(String serverAddr) {
        this.registerAddr = serverAddr;
    }

    @Override
    public void setReactors(int size) {
        this.reactors = size;

    }

    @Override
    public void setServerAddr(String ip, int port) {
        this.ip = ip;
        this.port = port;

    }

}

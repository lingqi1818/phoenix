package com.fangcloud.phoenix.server;

import java.util.ArrayList;
import java.util.List;

import com.fangcloud.phoenix.common.utils.PropertiesHelper;

/**
 * Phoenix服务器启动入口
 * 
 * @author chenke
 * @date 2017年4月1日 下午5:12:59
 */
public class PhoenixStarter {

    public static void main(String[] args) throws InterruptedException {
        ServerConfig config = getServerConfig();
        //BufferPool.createPool(config.getBufferSize(), 4096, config.getReactors());
        PhoenixServer server = new DefaultPhoenixServer();
        server.setServices(config.getServiers());
        server.setReactors(config.getReactors());
        server.setRegisterAddr(config.getRegisterAddr());
        server.setServerAddr(config.getIp(), config.getPort());
        server.start();
        Thread.currentThread().join();
    }

    private static ServerConfig getServerConfig() {
        ServerConfig config = new ServerConfig();
        PropertiesHelper helper = new PropertiesHelper("phoenix.properties");
        String[] services = helper.getProp("phoenix.serviers").replaceAll(";", ",").split(",");
        List<String> serviceList = new ArrayList<String>();
        if (services != null) {
            for (String service : services) {
                serviceList.add(service);
            }
        }
        config.setServiers(serviceList);
        config.setBufferSize(helper.getLongNumberProp("phoenix.bufferSize", 4096000));
        config.setIp(helper.getProp("phoenix.ip", "127.0.0.1"));
        config.setPort(helper.getIntNumberProp("phoenix.port", 4096000));
        config.setReactors(helper.getIntNumberProp("phoenix.reactors", 2));
        config.setRegisterAddr(helper.getProp("phoenix.registerAddr", "127.0.0.1:2181"));
        return config;
    }

    private static class ServerConfig {
        private long         bufferSize;
        private String       ip;
        private int          port;
        private List<String> serviers;
        private String       registerAddr;
        private int          reactors;

        public long getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(long bufferSize) {
            this.bufferSize = bufferSize;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public List<String> getServiers() {
            return serviers;
        }

        public void setServiers(List<String> serviers) {
            this.serviers = serviers;
        }

        public String getRegisterAddr() {
            return registerAddr;
        }

        public void setRegisterAddr(String registerAddr) {
            this.registerAddr = registerAddr;
        }

        public int getReactors() {
            return reactors;
        }

        public void setReactors(int reactors) {
            this.reactors = reactors;
        }

    }

}

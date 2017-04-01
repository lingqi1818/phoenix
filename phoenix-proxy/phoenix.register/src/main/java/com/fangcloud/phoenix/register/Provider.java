package com.fangcloud.phoenix.register;

/**
 * 服务提供者
 * 
 * @author chenke
 * @date 2017年3月20日 下午4:30:29
 */
public class Provider {
    private String protocol;
    private String serviceName;
    private String ip;
    private int    port;

    public String getAddress() {
        return ip + ":" + port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

}

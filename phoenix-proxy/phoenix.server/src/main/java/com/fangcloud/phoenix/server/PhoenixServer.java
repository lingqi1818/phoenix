package com.fangcloud.phoenix.server;

import java.util.List;

/**
 * Phoenix代理服务器：</br>
 * 1.该服务器在Tcp层解析hessian请求地址后，进行透明代理和转发。</br>
 * 2.与zk集群进行通信，发现并维护与hessian服务之间的连接，并且实现负载平衡。
 *
 * @author chenke
 * @date 2017年3月17日 下午5:29:21
 */
public interface PhoenixServer {
    /**
     * 服务器启动
     */
    public void start();

    /**
     * 服务器关闭
     */
    public void stop();

    /**
     * 设置代理服务列表
     * 
     * @param serviceList
     */
    public void setServices(List<String> serviceList);

    /**
     * 设置注册中心地址
     * 
     * @param serverAddr
     */
    public void setRegisterAddr(String serverAddr);

    /**
     * 设置reactor数量
     * 
     * @param size
     */
    public void setReactors(int size);

    /**
     * 设置服务器监听Ip和地址
     * 
     * @param ip
     * @param port
     */
    public void setServerAddr(String ip, int port);

}

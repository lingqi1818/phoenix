package com.fangcloud.phoenix.register;

import java.util.List;

/**
 * dubbo服务注册中心
 * 
 * @author chenke
 * @date 2017年3月20日 下午4:19:00
 */
public interface Register {
    /**
     * 根据服务名称获取服务提供者列表
     * 
     * @param serviceName
     * @return
     */
    public List<Provider> getProvider(String serviceName) throws RegisterException;

    /**
     * 注册指定服务的消费者
     * 
     * @param serviceName
     */
    public void registerCustomer(String serviceName) throws RegisterException;

    /**
     * 注册事件监听器
     * 
     * @param listener
     */
    public void addListener(Listener listener);

    /**
     * 注册中心事件监听器
     * 
     * @author chenke
     * @date 2017年3月24日 下午3:26:51
     */
    public static interface Listener {
        /**
         * 下发事件通知
         * 
         * @param event
         */
        public void fire();
    }
}

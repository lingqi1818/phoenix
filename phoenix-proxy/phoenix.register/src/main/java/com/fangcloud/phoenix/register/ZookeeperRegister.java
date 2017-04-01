package com.fangcloud.phoenix.register;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.register.util.IPUtils;
import com.fangcloud.phoenix.register.util.PathUtils;

/**
 * 基于zk的注册中心服务
 * 
 * @author chenke
 * @date 2017年3月20日 下午4:36:19
 */
public class ZookeeperRegister implements Register, ZKClient {
    private final static Logger            LOGGER    = LoggerFactory
            .getLogger(ZookeeperRegister.class);
    private CuratorFramework               client;
    private volatile boolean               isStart   = false;

    private CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    public ZookeeperRegister(String zkServer) throws RegisterException {
        initZkClient(zkServer);
    }

    private void initZkClient(String zkServer) throws RegisterException {
        if (StringUtils.isEmpty(zkServer)) {
            LOGGER.error("zkServer address is  null !!!");
            throw new RegisterException("zkServer address is  null !!!");
        }
        client = CuratorFrameworkFactory.builder().connectString(zkServer)
                .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000)).connectionTimeoutMs(5000)
                .build();
        start();
    }

    @Override
    public List<Provider> getProvider(final String serviceName) throws RegisterException {
        List<Provider> providers = new ArrayList<Provider>();
        try {
            if (client.checkExists().forPath(PathUtils.dubboRootPath()) == null
                    || client.checkExists().forPath(PathUtils.servicePath(serviceName)) == null
                    || client.checkExists().forPath(PathUtils.providersPath(serviceName)) == null) {
                LOGGER.error("the providers path is not exists in zk:"
                        + PathUtils.providersPath(serviceName));
                return providers;
            }
            List<String> list = client.getChildren().forPath(PathUtils.providersPath(serviceName));
            client.getData().usingWatcher(new CuratorWatcher() {

                @Override
                public void process(WatchedEvent event) throws Exception {
                    if (event.getType() == EventType.NodeChildrenChanged) {
                        for (Listener listener : listeners) {
                            listener.fire();
                        }
                        client.getData().usingWatcher(this)
                                .forPath(PathUtils.providersPath(serviceName));
                        return;
                    }

                    if (event.getType() == EventType.NodeDeleted) {
                        for (Listener listener : listeners) {
                            listener.fire();
                        }
                        return;
                    }
                }

            }).forPath(PathUtils.providersPath(serviceName));
            parseProviderList(providers, list);
        } catch (Exception e) {
            throw new RegisterException("get provider from zookeeper failed !!!", e);
        }
        return providers;
    }

    private void parseProviderList(List<Provider> providers, List<String> list) {
        if (list == null || list.size() <= 0) {
            return;
        }
        for (String str : list) {
            Provider provider = parseSingleProvider(str);
            if (provider != null) {
                providers.add(provider);
            }
        }
    }

    private Provider parseSingleProvider(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }

        try {
            String data = URLDecoder.decode(str, "utf-8");
            String d_str[] = data.split("\\?");
            if (d_str.length >= 1) {
                String url = d_str[0];
                String[] d_url = url.split("/");
                if (d_url.length == 4) {
                    String ip_with_port = d_url[2];
                    String serviceName = d_url[3];
                    String[] d_ip_with_port = ip_with_port.split(":");
                    if (d_ip_with_port.length == 2 && !StringUtils.isEmpty(serviceName)) {
                        Provider provider = new Provider();
                        provider.setProtocol(d_url[0].substring(0, d_url[0].length() - 1));
                        provider.setIp(d_ip_with_port[0]);
                        provider.setPort(Integer.valueOf(d_ip_with_port[1]));
                        provider.setServiceName(serviceName);
                        return provider;
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("parse provider data from zk error,reason is:" + e.getMessage());
        }
        return null;
    }

    @Override
    public void registerCustomer(String serviceName) throws RegisterException {
        try {
            if (client.checkExists().forPath(PathUtils.dubboRootPath()) == null
                    || client.checkExists().forPath(PathUtils.servicePath(serviceName)) == null
                    || client.checkExists().forPath(PathUtils.consumersPath(serviceName)) == null) {
                LOGGER.error("the consumers path is not exists in zk:"
                        + PathUtils.providersPath(serviceName));
                return;
            }
            String customerPath = "/dubbo/" + serviceName + "/consumers/" + URLEncoder.encode(
                    "consumer://" + IPUtils.getServerIp() + "/" + serviceName + "'?", "utf-8");
            if (client.checkExists().forPath(customerPath) == null) {
                client.create().withMode(CreateMode.EPHEMERAL).withACL(Ids.OPEN_ACL_UNSAFE)
                        .forPath(customerPath);
            }
        } catch (Exception ex) {
            throw new RegisterException("register customer to zookeeper failed !!!", ex);
        }

    }

    @Override
    public void start() {
        if (!isStart)
            client.start();

    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

}

package com.fangcloud.phoenix.server.connection;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.common.protocol.Protocol;
import com.fangcloud.phoenix.register.Provider;
import com.fangcloud.phoenix.register.Register;
import com.fangcloud.phoenix.register.RegisterException;
import com.fangcloud.phoenix.register.ZookeeperRegister;
import com.fangcloud.phoenix.server.io.NIOReactorPool;

/**
 * 服务提供者连接池
 * 
 * @author chenke
 * @date 2017年3月21日 下午5:31:31
 */
public class ProviderConnectionPool {
    private static final Logger                                              LOGGER              = LoggerFactory
            .getLogger(ProviderConnectionPool.class);
    private Register                                                         register;
    private ConcurrentHashMap<String, CopyOnWriteArraySet<String>>           serviceAddressMap   = new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<String>>           addressServiceMap   = new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();
    private ConcurrentHashMap<String, GenericObjectPool<ProviderConnection>> addressPoolMap      = new ConcurrentHashMap<String, GenericObjectPool<ProviderConnection>>();

    private int                                                              minIdle             = 1;
    private int                                                              maxIdle             = 5;
    private int                                                              maxTotal            = 20;
    private int                                                              maxWait             = 5000;
    private Lock                                                             poolLock            = new ReentrantLock();
    private ConcurrentHashMap<String, AtomicInteger>                         nextAddressCountMap = new ConcurrentHashMap<String, AtomicInteger>();

    private static ProviderConnectionPool                                    pool;
    private NIOReactorPool                                                   reactorPool;

    public static ProviderConnectionPool makePool(String registerServer,
                                                  final List<String> services,
                                                  NIOReactorPool reactorPool) {

        if (pool == null) {
            pool = new ProviderConnectionPool(registerServer, services, reactorPool);
        }
        return pool;
    }

    public static ProviderConnectionPool getPoolInstance() {
        return pool;
    }

    private ProviderConnectionPool(String registerServer, final List<String> services,
                                   NIOReactorPool reactorPool) {
        try {
            this.reactorPool = reactorPool;
            this.register = new ZookeeperRegister(registerServer);
            register.addListener(new Register.Listener() {
                @Override
                public void fire() {
                    poolLock.lock();
                    try {
                        for (String service : services) {
                            try {
                                List<Provider> providers = register.getProvider(service);
                                tryToMakeNewPool(service, providers);
                                tryToRemoveOldPool(service, providers);
                            } catch (RegisterException e) {
                                LOGGER.error("fire listener get provider error:", e);
                            }
                        }
                    } finally {
                        poolLock.unlock();
                    }
                }

                /*
                 * 销毁逻辑： 假如该服务对应地址列表不存在与providers的列表中，
                 * 那么查找该地址是否还对应其他服务，假如仅仅对应该服务，那么就销毁该地址的连接池
                 */
                private void tryToRemoveOldPool(String service, List<Provider> providers) {
                    //serviceAddressMap
                    //addressServiceMap
                    //addressPoolMap
                    CopyOnWriteArraySet<String> addressSet = serviceAddressMap.get(service);
                    if (addressSet == null) {
                        addressSet = new CopyOnWriteArraySet<String>();
                        serviceAddressMap.put(service, addressSet);
                    }

                    for (String addr : addressSet) {
                        if (!providersContain(addr, providers)) {
                            addressSet.remove(addr);

                            CopyOnWriteArraySet<String> serviceSet = addressServiceMap.get(addr);
                            if (serviceSet == null) {
                                serviceSet = new CopyOnWriteArraySet<String>();
                                addressServiceMap.put(addr, serviceSet);
                            }

                            if ((serviceSet.contains(service) && serviceSet.size() == 1)
                                    || serviceSet.size() <= 0) {
                                serviceSet.remove(service);
                                GenericObjectPool<ProviderConnection> pool = addressPoolMap
                                        .get(addr);
                                pool.close();
                                addressPoolMap.remove(addr);
                            }
                        }
                    }

                }

                private boolean providersContain(String addr, List<Provider> providers) {
                    for (Provider p : providers) {
                        if (addr.equalsIgnoreCase(p.getAddress())) {
                            return true;
                        }
                    }
                    return false;
                }

                /*
                 * 创建逻辑 假如该服务对应的地址目前不包含providers中的某些地址， 那么将这些地址创建新的连接池
                 */
                private void tryToMakeNewPool(String service, List<Provider> providers) {
                    //serviceAddressMap
                    //addressServiceMap
                    //addressPoolMap
                    CopyOnWriteArraySet<String> addressSet = serviceAddressMap.get(service);
                    if (addressSet == null) {
                        addressSet = new CopyOnWriteArraySet<String>();
                        serviceAddressMap.put(service, addressSet);
                    }
                    for (Provider p : providers) {
                        CopyOnWriteArraySet<String> serviceSet = addressServiceMap
                                .get(p.getAddress());
                        if (serviceSet == null) {
                            serviceSet = new CopyOnWriteArraySet<String>();
                            addressServiceMap.put(p.getAddress(), serviceSet);

                        }
                        if (!addressSet.contains(p.getAddress())) {
                            addressSet.add(p.getAddress());
                        }

                        if (!serviceSet.contains(service)) {
                            serviceSet.add(service);
                        }

                        if (addressPoolMap.get(p.getAddress()) == null) {
                            makeGenericObjectPool(p.getAddress(), p.getIp(), p.getPort());
                        }

                    }

                }
            });

            poolLock.lock();
            initProvidersConnectionPoolByServices(services);
        } catch (RegisterException e) {
            LOGGER.error("init Provider Connection Pool error:", e);
        } finally {
            poolLock.unlock();
        }
    }

    private void initProvidersConnectionPoolByServices(List<String> services) {
        for (String service : services) {
            try {
                initSingleProviderConnectionPool(service);
            } catch (Exception ex) {
                LOGGER.error("init " + service + " provider connection pool error:", ex);
            }
        }
    }

    private void initSingleProviderConnectionPool(String service) throws RegisterException {
        List<Provider> providers = register.getProvider(service);
        for (Provider provider : providers) {
            //暂时不支持hessian外的其他协议
            if (!provider.getProtocol().equalsIgnoreCase(Protocol.HESSIAN.getValue())) {
                continue;
            }
            String address = provider.getAddress();
            CopyOnWriteArraySet<String> sset = addressServiceMap.get(address);
            if (sset == null) {
                sset = new CopyOnWriteArraySet<String>();
                addressServiceMap.put(address, sset);
            }
            sset.add(service);
            CopyOnWriteArraySet<String> aset = serviceAddressMap.get(service);
            if (aset == null) {
                aset = new CopyOnWriteArraySet<String>();
                serviceAddressMap.put(service, aset);
            }
            aset.add(address);
            makeGenericObjectPool(address, provider.getIp(), provider.getPort());
        }
    }

    private void makeGenericObjectPool(String address, String ip, int port) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(minIdle);
        config.setMaxIdle(maxIdle);
        config.setMaxTotal(maxTotal);
        config.setMaxWaitMillis(maxWait);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setNumTestsPerEvictionRun(1);
        config.setJmxEnabled(false);
        config.setSoftMinEvictableIdleTimeMillis(30000);//代表空闲时间
        config.setTimeBetweenEvictionRunsMillis(30000);//代表回收周期
        GenericObjectPool<ProviderConnection> pool = new GenericObjectPool<ProviderConnection>(
                new ProviderConnectionFactory(ip, port, Protocol.HESSIAN, reactorPool), config);
        addressPoolMap.put(address, pool);
    }

    public ProviderConnection borrowConnection(String serviceName) {
        Set<String> addresses = serviceAddressMap.get(serviceName);
        String nextAddress = getNextAddress(addresses, serviceName);
        try {
            HessianProviderConnection pconn = (HessianProviderConnection) addressPoolMap
                    .get(nextAddress).borrowObject();
            return pconn;
        } catch (Exception e) {
            LOGGER.error("borrow " + serviceName + " " + nextAddress + " pool failed,", e);
        }
        return null;
    }

    private String getNextAddress(Set<String> addresses, String serviceName) {
        AtomicInteger count = nextAddressCountMap.get(serviceName);
        if (count == null) {
            count = new AtomicInteger(0);
            nextAddressCountMap.put(serviceName, count);
        }
        int val = count.incrementAndGet() % addresses.size();
        return (String) addresses.toArray()[val];
    }

    public void returnConnection(ProviderConnection pconn) {
        addressPoolMap.get(pconn.getAddress() + ":" + pconn.getPort()).returnObject(pconn);
    }

}

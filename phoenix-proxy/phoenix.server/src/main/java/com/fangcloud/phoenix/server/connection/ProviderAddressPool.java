package com.fangcloud.phoenix.server.connection;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fangcloud.phoenix.common.protocol.Protocol;
import com.fangcloud.phoenix.register.Provider;
import com.fangcloud.phoenix.register.Register;
import com.fangcloud.phoenix.register.RegisterException;
import com.fangcloud.phoenix.register.ZookeeperRegister;

/**
 * 服务提供者连接池
 * 
 * @author chenke
 * @date 2017年3月21日 下午5:31:31
 */
public class ProviderAddressPool {
    private static final Logger                                    LOGGER              = LoggerFactory
            .getLogger(ProviderAddressPool.class);
    private Register                                               register;
    private ConcurrentHashMap<String, CopyOnWriteArraySet<String>> serviceAddressMap   = new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();

    private ConcurrentHashMap<String, AtomicInteger>               nextAddressCountMap = new ConcurrentHashMap<String, AtomicInteger>();
    private Lock                                                   poolLock            = new ReentrantLock();
    private static ProviderAddressPool                             pool;

    public static ProviderAddressPool makePool(String registerServer, final List<String> services) {

        if (pool == null) {
            pool = new ProviderAddressPool(registerServer, services);
        }
        return pool;
    }

    public static ProviderAddressPool getPoolInstance() {
        return pool;
    }

    private ProviderAddressPool(String registerServer, final List<String> services) {
        try {
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
                 * 销毁逻辑： 假如该服务对应地址列表不存在与providers的列表中,那么就移除它
                 */
                private void tryToRemoveOldPool(String service, List<Provider> providers) {
                    CopyOnWriteArraySet<String> addressSet = serviceAddressMap.get(service);
                    if (addressSet == null) {
                        addressSet = new CopyOnWriteArraySet<String>();
                        serviceAddressMap.put(service, addressSet);
                    }

                    for (String addr : addressSet) {
                        if (!providersContain(addr, providers)) {
                            addressSet.remove(addr);
                            nextAddressCountMap.remove(addr);
                        }
                    }

                }

                private boolean providersContain(String addr, List<Provider> providers) {
                    for (Provider p : providers) {
                        if (addr.equalsIgnoreCase(p.getAddress())
                                && p.getProtocol().equalsIgnoreCase(Protocol.HESSIAN.getValue())) {
                            return true;
                        }
                    }
                    return false;
                }

                /*
                 * 创建逻辑 假如该服务对应的地址目前不包含providers中的某些地址，则添加它
                 */
                private void tryToMakeNewPool(String service, List<Provider> providers) {
                    CopyOnWriteArraySet<String> addressSet = serviceAddressMap.get(service);
                    if (addressSet == null) {
                        addressSet = new CopyOnWriteArraySet<String>();
                        serviceAddressMap.put(service, addressSet);
                    }
                    for (Provider p : providers) {
                        if (!addressSet.contains(p.getAddress())
                                && p.getProtocol().equalsIgnoreCase(Protocol.HESSIAN.getValue())) {
                            addressSet.add(p.getAddress());
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
            CopyOnWriteArraySet<String> aset = serviceAddressMap.get(service);
            if (aset == null) {
                aset = new CopyOnWriteArraySet<String>();
                serviceAddressMap.put(service, aset);
            }
            aset.add(address);
        }
    }

    public String getAddressByServiceName(String serviceName) {
        Set<String> addresses = serviceAddressMap.get(serviceName);
        return getNextAddress(addresses, serviceName);
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

}

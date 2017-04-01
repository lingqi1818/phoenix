package com.fangcloud.phoenix.register.test;

import java.util.List;

import com.fangcloud.phoenix.register.Provider;
import com.fangcloud.phoenix.register.RegisterException;
import com.fangcloud.phoenix.register.ZKClient;
import com.fangcloud.phoenix.register.ZookeeperRegister;

import junit.framework.TestCase;

public class ZookeeperRegisterTest extends TestCase {
    private ZookeeperRegister register;

    public void setUp() throws RegisterException {
        register = new ZookeeperRegister("121.40.169.141:2181");
    }

    public void testGetProvider() throws RegisterException {
        List<Provider> providers = register.getProvider("com.fangcloud.phoenix.test.HelloService");
        if (providers.size() > 0) {
            for (Provider p : providers) {
                System.out.println("protocol:" + p.getProtocol());
                System.out.println("serviceName:" + p.getServiceName());
                System.out.println("serviceIp:" + p.getIp());
                System.out.println("servicePort:" + p.getPort());
            }
        }
    }

    public void testRegisterCustomer() throws RegisterException {
        register.registerCustomer("com.fangcloud.phoenix.test.HelloService");
    }

    public void tearDown() {
        ZKClient client = register;
        client.close();
    }

}

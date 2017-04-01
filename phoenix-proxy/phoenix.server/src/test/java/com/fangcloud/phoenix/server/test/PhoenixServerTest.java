package com.fangcloud.phoenix.server.test;

import java.util.ArrayList;
import java.util.List;

import com.fangcloud.phoenix.server.DefaultPhoenixServer;
import com.fangcloud.phoenix.server.PhoenixServer;
import com.fangcloud.phoenix.server.buffer.BufferPool;

public class PhoenixServerTest {

    public static void main(String args[]) throws Exception {
        //PhoenixServer server = new JettyPhoenixServer();
        BufferPool.createPool(40960, 4096, 2);
        PhoenixServer server = new DefaultPhoenixServer();
        List<String> services = new ArrayList<String>();
        services.add("com.fangcloud.phoenix.test.HelloService");
        server.setServices(services);
        server.setReactors(16);
        server.setRegisterAddr("");
        server.start();
    }
}

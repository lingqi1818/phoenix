package com.fangcloud.phoenix.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.hessian.client.HessianProxyFactory;

public class HessianTest {
    private static volatile int index;

    public static void main(String[] args) throws Exception {
        testConnection();
    }

    private static void testConnection() throws Exception {
        final String url = "http://127.0.0.1:9898/com.fangcloud.phoenix.test.HelloService";

        final AtomicLong total = new AtomicLong(0);
        final long count = 10000;
        int threadCount = 2;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new Runnable() {
                HessianProxyFactory pf = new HessianProxyFactory();
                final HelloService  hs = (HelloService) pf.create(url);

                @Override
                public void run() {
                    long itotal = 0;
                    for (long i = 0; i < count; i++) {
                        long start = System.currentTimeMillis();
                        int x = index++;
                        Student s = hs.hello(x);
                        //System.out.println(x);
                        if (x != s.getSex()) {
                            System.out.println("assert false !");
                        }
                        itotal += System.currentTimeMillis() - start;
                    }
                    total.set(itotal);
                    latch.countDown();

                }
            });
            t.start();
        }

        latch.await();
        System.out.println("avg time:" + total.get() / count * threadCount);

    }
    //1.测试1个连接，多次请求
    //2.测试多个连接多次请求

}

package com.fangcloud.phoenix.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

    public static void main(String[] args) throws Exception {
        @SuppressWarnings("resource")
        ApplicationContext ac = new ClassPathXmlApplicationContext("dubbo-provider.xml");
        HelloService s = (HelloService) ac.getBean("helloServiceR");
        for (int i = 0; i < 10000; i++) {
            long start = System.currentTimeMillis();
            System.out.println("remote student name is:" + s.hello(10240).getName());
            System.out.println("invoke time:" + (System.currentTimeMillis() - start));
        }

        Thread.currentThread().join();
    }

}

package com.fangcloud.phoenix.test;

public class HelloServiceImpl implements HelloService {

    @Override
    public Student hello(int id) {
        //System.out.println("id is:" + id);
        Student s = new Student();
        s.setName("zhangsan");
        s.setSex(id);
        return s;
    }

}

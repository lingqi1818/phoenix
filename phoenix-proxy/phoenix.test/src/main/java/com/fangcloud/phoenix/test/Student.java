package com.fangcloud.phoenix.test;

import java.io.Serializable;

public class Student implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String            name;
    private int               sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

}

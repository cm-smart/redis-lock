package com.chen.redissonTest.entity;

import java.io.Serializable;

public class City implements Serializable {
    private String name;
    private String province;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }
}

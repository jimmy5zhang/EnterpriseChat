package com.zkjinshi.company.vo;

import java.io.Serializable;

/**
 * 授权实体类
 *
 */
public class AuthVo implements Serializable {

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

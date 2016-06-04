package com.zkjinshi.company.response;

import com.zkjinshi.company.vo.AuthVo;

/**
 * 授权响应类
 *
 */
public class AuthResponse extends BaseResponse {

    private AuthVo data;

    public AuthVo getData() {
        return data;
    }

    public void setData(AuthVo data) {
        this.data = data;
    }
}

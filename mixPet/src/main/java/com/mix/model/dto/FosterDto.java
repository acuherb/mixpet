package com.mix.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mix.model.entity.Foster;

public class FosterDto extends Foster {
    /**
     * 宠物姓名
     */
    private String petName;
    /**
     * 用户姓名。匿名展示墙的 SQL 不查这一列，NON_NULL 序列化时直接不下发，
     * 避免把字段名（乃至后续误填的值）暴露给未登录访客。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String clientName;

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}

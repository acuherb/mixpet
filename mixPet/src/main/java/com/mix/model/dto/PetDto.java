package com.mix.model.dto;

import com.mix.model.entity.Pet;

public class PetDto extends Pet {
    /**
     * 主人姓名
     */
    private String clientName;

    /**
     * 主人手机号（挂号/检索场景展示）
     */
    private String clientTel;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientTel() {
        return clientTel;
    }

    public void setClientTel(String clientTel) {
        this.clientTel = clientTel;
    }
}

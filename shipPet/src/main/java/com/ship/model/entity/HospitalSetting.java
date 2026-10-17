package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 医院基础信息（单行配置，固定 id=1）：名称/地址/电话/logo/营业时间/简介
 */
@Data
@TableName("hospital_setting")
public class HospitalSetting implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private String name;
    private String address;
    private String phone;
    /** logo 文件名（相对 common/download?name=） */
    private String logo;
    /** 品牌主题色(HEX，如 #1E6FA8)，注入 CSS 变量 --brand 全局生效 */
    private String brandColor;
    private String bizHours;
    private String intro;
}

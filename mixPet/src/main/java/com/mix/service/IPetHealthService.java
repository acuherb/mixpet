package com.mix.service;

import com.mix.model.vo.PetHealth;

/**
 * 一页式宠物健康档案 Service
 */
public interface IPetHealthService {
    /** 聚合某宠物的健康档案；不存在返回 null */
    PetHealth get(Integer petId);
}

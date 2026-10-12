package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.model.entity.Drug;

import java.util.List;

/**
 * 药品 Service
 */
public interface IDrugService extends IService<Drug> {
    boolean deleteById(Integer drugId);

    boolean deleteByIds(List<Integer> idGroup);

    IPage<Drug> selectByPage(String drugName, String drugType, int numPage, int pageSize);

    /** 启用中的药品（医生开方下拉用） */
    List<Drug> listActive();
}

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

    /** 启用中的药品（医生开方下拉用，不含疫苗） */
    List<Drug> listActive();

    /** 启用中的疫苗（免疫登记下拉用，即 drug_type=疫苗） */
    List<Drug> listVaccine();
}

package com.ship.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.model.entity.Vaccine;

/**
 * 疫苗字典 Service
 */
public interface IVaccineService extends IService<Vaccine> {
    /** 删除：被接种档案引用则改停用 */
    boolean deleteVaccine(Integer id);
}
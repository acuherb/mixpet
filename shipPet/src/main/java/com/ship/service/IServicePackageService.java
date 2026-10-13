package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.model.entity.ServicePackage;

import java.util.List;

/**
 * 套餐 Service
 */
public interface IServicePackageService extends IService<ServicePackage> {
    /** 分页 */
    IPage<ServicePackage> selectByPage(String packageName, String category, int numPage, int pageSize);

    /** 启用中的套餐（接诊下拉用） */
    List<ServicePackage> listActive();

    /** 删除（软删除，历史套餐记录保留快照） */
    boolean deleteById(Integer packageId);
}

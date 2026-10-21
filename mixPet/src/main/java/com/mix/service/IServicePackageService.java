package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.PackageItem;
import com.mix.model.entity.ServicePackage;

import java.util.List;

/**
 * 服务套餐字典服务
 */
public interface IServicePackageService extends IService<ServicePackage> {

    IPage<ServicePackage> selectByPage(String packageName, String category, int numPage, int pageSize);

    List<ServicePackage> listActive();

    boolean deleteById(Integer packageId);

    /** 新增/修改套餐（主子表一个事务：先删子表再按传入 items 重插） */
    boolean saveWithItems(ServicePackage p);

    /** 切换启用状态（列表行内开关用） */
    boolean switchActive(Integer packageId, Boolean active);
}

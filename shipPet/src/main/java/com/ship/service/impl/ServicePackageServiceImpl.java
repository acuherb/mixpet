package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.mapper.ServicePackageMapper;
import com.ship.model.entity.ServicePackage;
import com.ship.service.IServicePackageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 套餐 Service 实现
 */
@Service
public class ServicePackageServiceImpl extends ServiceImpl<ServicePackageMapper, ServicePackage> implements IServicePackageService {

    @Override
    public IPage<ServicePackage> selectByPage(String packageName, String category, int numPage, int pageSize) {
        return this.lambdaQuery()
                .and(packageName != null && !packageName.isBlank(), w -> w.like(ServicePackage::getPackageName, packageName))
                .eq(category != null && !category.isBlank(), ServicePackage::getCategory, category)
                .orderByAsc(ServicePackage::getCategory)
                .orderByAsc(ServicePackage::getPackageId)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public List<ServicePackage> listActive() {
        return this.lambdaQuery().eq(ServicePackage::getActive, true)
                .orderByAsc(ServicePackage::getPackageId).list();
    }

    @Override
    public boolean deleteById(Integer packageId) {
        return this.lambdaUpdate()
                .eq(ServicePackage::getPackageId, packageId)
                .set(ServicePackage::getIsDel, System.currentTimeMillis())
                .update();
    }
}

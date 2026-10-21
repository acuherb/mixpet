package com.mix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.PackageItemMapper;
import com.mix.mapper.ServicePackageMapper;
import com.mix.model.entity.PackageItem;
import com.mix.model.entity.ServicePackage;
import com.mix.service.IServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 套餐 Service 实现（主子表：套餐 + 包含项目）
 */
@Service
public class ServicePackageServiceImpl extends ServiceImpl<ServicePackageMapper, ServicePackage> implements IServicePackageService {

    @Autowired
    private PackageItemMapper packageItemMapper;

    @Override
    public IPage<ServicePackage> selectByPage(String packageName, String category, int numPage, int pageSize) {
        IPage<ServicePackage> page = this.lambdaQuery()
                .and(packageName != null && !packageName.isBlank(), w -> w.like(ServicePackage::getPackageName, packageName))
                .eq(category != null && !category.isBlank(), ServicePackage::getCategory, category)
                .orderByAsc(ServicePackage::getCategory)
                .orderByAsc(ServicePackage::getPackageId)
                .page(new Page<>(numPage, pageSize));
        fillItems(page.getRecords());
        return page;
    }

    @Override
    public List<ServicePackage> listActive() {
        List<ServicePackage> list = this.lambdaQuery().eq(ServicePackage::getActive, true)
                .orderByAsc(ServicePackage::getPackageId).list();
        fillItems(list);
        return list;
    }

    /** 批量组装套餐包含的项目（一次 IN 查询，避免 N+1） */
    private void fillItems(List<ServicePackage> packages) {
        if (packages == null || packages.isEmpty()) return;
        List<Integer> ids = packages.stream().map(ServicePackage::getPackageId).collect(Collectors.toList());
        List<PackageItem> items = packageItemMapper.selectList(new LambdaQueryWrapper<PackageItem>()
                .in(PackageItem::getPackageId, ids)
                .eq(PackageItem::getIsDel, 0)
                .orderByAsc(PackageItem::getId));
        Map<Integer, List<PackageItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(PackageItem::getPackageId));
        packages.forEach(p -> p.setItems(grouped.getOrDefault(p.getPackageId(), Collections.emptyList())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithItems(ServicePackage p) {
        boolean isNew = p.getPackageId() == null;
        boolean ok = isNew ? this.save(p) : this.updateById(p);
        if (!ok) return false;
        rewriteItems(p.getPackageId(), p.getItems());
        return true;
    }

    /** 子表重写：先逻辑删旧行，再按传入重插（保证增删改一致） */
    private void rewriteItems(Integer packageId, List<PackageItem> items) {
        if (packageId == null) return;
        packageItemMapper.update(null, new LambdaUpdateWrapper<PackageItem>()
                .eq(PackageItem::getPackageId, packageId)
                .set(PackageItem::getIsDel, System.currentTimeMillis()));
        if (items == null || items.isEmpty()) return;
        for (PackageItem it : items) {
            if (it.getItemId() == null || it.getItemName() == null) continue;
            PackageItem row = new PackageItem();
            row.setPackageId(packageId);
            row.setItemType(it.getItemType());
            row.setItemId(it.getItemId());
            row.setItemName(it.getItemName());
            row.setItemPrice(it.getItemPrice());
            packageItemMapper.insert(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean switchActive(Integer packageId, Boolean active) {
        return this.lambdaUpdate()
                .eq(ServicePackage::getPackageId, packageId)
                .set(ServicePackage::getActive, active != null && active)
                .update();
    }

    @Override
    public boolean deleteById(Integer packageId) {
        return this.lambdaUpdate()
                .eq(ServicePackage::getPackageId, packageId)
                .set(ServicePackage::getIsDel, System.currentTimeMillis())
                .update();
    }
}

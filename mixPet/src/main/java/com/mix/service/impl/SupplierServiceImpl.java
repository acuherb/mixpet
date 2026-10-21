package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.SupplierMapper;
import com.mix.model.entity.Supplier;
import com.mix.service.ISupplierService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商 Service 实现
 */
@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements ISupplierService {
    @Override
    public IPage<Supplier> selectPage(String seaName, int numPage, int pageSize) {
        return this.lambdaQuery()
                .and(seaName != null && !seaName.isEmpty(), w -> w
                        .like(Supplier::getSupplierName, seaName)
                        .or().like(Supplier::getContact, seaName))
                .orderByAsc(Supplier::getSupplierId)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public List<Supplier> listActive() {
        return this.lambdaQuery().eq(Supplier::getActive, true)
                .orderByAsc(Supplier::getSupplierName).list();
    }
}

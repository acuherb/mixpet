package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.model.entity.Supplier;

import java.util.List;

/**
 * 供应商 Service
 */
public interface ISupplierService extends IService<Supplier> {
    IPage<Supplier> selectPage(String seaName, int numPage, int pageSize);

    /** 启用中的供应商（采购入库下拉用） */
    List<Supplier> listActive();
}

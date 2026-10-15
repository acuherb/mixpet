package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.entity.Drug;
import com.ship.model.entity.DrugRecord;

import java.time.LocalDate;
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

    /** 低库存预警列表（stock <= warningStock） */
    List<Drug> listWarning();

    /** 近效期预警列表（expireDate 在未来 days 天内，含当天，未过期） */
    List<Drug> listExpiring(int days);

    /** 入库：增加库存并记台账（可带供应商/批号），可同时更新有效期 */
    Result inbound(Integer drugId, Integer quantity, LocalDate expireDate, String note, Integer supplierId, String batchNo);

    /** 出入库台账分页 */
    IPage<DrugRecord> stockRecordPage(Integer drugId, int numPage, int pageSize);

    /** 记一条库存流水（发药出库/退款回库等由业务方调用） */
    void logStockChange(Integer drugId, String drugName, int changeType, int quantity, int before, int after, String note);
}

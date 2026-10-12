package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.mapper.DrugMapper;
import com.ship.model.entity.Drug;
import com.ship.service.IDrugService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 药品 Service 实现
 */
@Service
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug> implements IDrugService {

    @Override
    public boolean deleteById(Integer drugId) {
        return this.lambdaUpdate()
                .eq(Drug::getDrugId, drugId)
                .set(Drug::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Drug::getDrugId, idGroup)
                .set(Drug::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<Drug> selectByPage(String drugName, String drugType, int numPage, int pageSize) {
        return this.lambdaQuery()
                .and(drugName != null && !drugName.isEmpty(), w -> w
                        .like(Drug::getDrugName, drugName)
                        .or().like(Drug::getDrugSpec, drugName))
                .eq(drugType != null && !drugType.isEmpty(), Drug::getDrugType, drugType)
                .orderByAsc(Drug::getDrugName)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public List<Drug> listActive() {
        return this.lambdaQuery().eq(Drug::getActive, true).orderByAsc(Drug::getDrugName).list();
    }
}

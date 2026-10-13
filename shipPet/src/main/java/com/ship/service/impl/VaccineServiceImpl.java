package com.ship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.mapper.ImmuneRecordMapper;
import com.ship.mapper.VaccineMapper;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.Vaccine;
import com.ship.service.IVaccineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 疫苗字典 Service 实现
 */
@Service
public class VaccineServiceImpl extends ServiceImpl<VaccineMapper, Vaccine> implements IVaccineService {
    private final ImmuneRecordMapper immuneMapper;

    @Autowired
    public VaccineServiceImpl(ImmuneRecordMapper immuneMapper) {
        this.immuneMapper = immuneMapper;
    }

    @Override
    public boolean deleteVaccine(Integer id) {
        Vaccine vaccine = this.getById(id);
        if (vaccine == null) return false;
        Long ref = immuneMapper.selectCount(new LambdaQueryWrapper<ImmuneRecord>()
                .eq(ImmuneRecord::getVaccineId, id));
        if (ref != null && ref > 0) {
            return this.update(Wrappers.<Vaccine>lambdaUpdate()
                    .eq(Vaccine::getVaccineId, id).set(Vaccine::getIsDel, 1L));
        }
        return this.removeById(id);
    }
}
package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.RecordTemplateMapper;
import com.mix.model.entity.RecordTemplate;
import com.mix.service.IRecordTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecordTemplateServiceImpl extends ServiceImpl<RecordTemplateMapper, RecordTemplate> implements IRecordTemplateService {
    @Override
    public IPage<RecordTemplate> selectPage(String seaName, int numPage, int pageSize) {
        return this.lambdaQuery()
                .and(seaName != null && !seaName.isEmpty(), w -> w
                        .like(RecordTemplate::getName, seaName)
                        .or().like(RecordTemplate::getCategory, seaName))
                .orderByAsc(RecordTemplate::getTemplateId)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public List<RecordTemplate> listActive() {
        return this.lambdaQuery().eq(RecordTemplate::getActive, true)
                .orderByAsc(RecordTemplate::getTemplateId).list();
    }
}

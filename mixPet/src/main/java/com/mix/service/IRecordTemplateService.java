package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.RecordTemplate;

import java.util.List;

/**
 * 电子病历模板 Service
 */
public interface IRecordTemplateService extends IService<RecordTemplate> {
    IPage<RecordTemplate> selectPage(String seaName, int numPage, int pageSize);

    /** 启用中的模板（接诊下拉用） */
    List<RecordTemplate> listActive();
}

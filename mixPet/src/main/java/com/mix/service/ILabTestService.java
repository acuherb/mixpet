package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.LabTest;

/**
 * 检验项字典 Service
 */
public interface ILabTestService extends IService<LabTest> {
    /** 字典分页 */
    IPage<LabTest> selectByPage(String testName, String testGroup, int numPage, int pageSize);

    /** 删除：被历史化验单引用时改为停用 */
    boolean deleteById(Integer id);
}
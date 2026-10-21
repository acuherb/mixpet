package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.ImageTest;

/**
 * 影像项目 Service
 */
public interface IImageTestService extends IService<ImageTest> {
    /** 删除：被历史申请单引用则改停用 */
    boolean deleteTest(Integer id);
}
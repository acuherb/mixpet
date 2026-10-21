package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.ImageOrderMapper;
import com.mix.mapper.ImageTestMapper;
import com.mix.model.entity.ImageOrder;
import com.mix.model.entity.ImageTest;
import com.mix.service.IImageTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 影像项目 Service 实现
 */
@Service
public class ImageTestServiceImpl extends ServiceImpl<ImageTestMapper, ImageTest> implements IImageTestService {
    private final ImageOrderMapper orderMapper;

    @Autowired
    public ImageTestServiceImpl(ImageOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public boolean deleteTest(Integer id) {
        ImageTest test = this.getById(id);
        if (test == null) return false;
        Long ref = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImageOrder>()
                        .eq(ImageOrder::getItemId, id));
        if (ref != null && ref > 0) {
            // 已被历史申请单引用，软删除（保留历史快照，不再出现在新开单可选列表）
            return this.update(com.baomidou.mybatisplus.core.toolkit.Wrappers.<ImageTest>lambdaUpdate()
                    .eq(ImageTest::getItemId, id).set(ImageTest::getIsDel, 1L));
        }
        return this.removeById(id);
    }
}
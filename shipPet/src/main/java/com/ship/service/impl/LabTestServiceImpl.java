package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.mapper.LabOrderItemMapper;
import com.ship.mapper.LabTestMapper;
import com.ship.model.entity.LabOrderItem;
import com.ship.model.entity.LabTest;
import com.ship.service.ILabTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检验项字典 Service 实现
 */
@Service
public class LabTestServiceImpl extends ServiceImpl<LabTestMapper, LabTest> implements ILabTestService {
    private final LabOrderItemMapper labOrderItemMapper;

    @Autowired
    public LabTestServiceImpl(LabOrderItemMapper labOrderItemMapper) {
        this.labOrderItemMapper = labOrderItemMapper;
    }

    @Override
    public IPage<LabTest> selectByPage(String testName, String testGroup, int numPage, int pageSize) {
        boolean hasName = testName != null && !testName.isBlank();
        boolean hasGroup = testGroup != null && !testGroup.isBlank();
        return this.lambdaQuery()
                .like(hasName, LabTest::getTestName, testName)
                .eq(hasGroup, LabTest::getTestGroup, testGroup)
                .orderByAsc(LabTest::getTestGroup)
                .orderByDesc(LabTest::getCreateTime)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public boolean deleteById(Integer id) {
        Long used = labOrderItemMapper.selectCount(
                Wrappers.<LabOrderItem>lambdaQuery().eq(LabOrderItem::getTestId, id));
        if (used != null && used > 0) {
            // 已被历史化验单引用，改为停用而非删除
            return this.lambdaUpdate()
                    .eq(LabTest::getTestId, id)
                    .set(LabTest::getActive, false)
                    .update();
        }
        return this.removeById(id);
    }
}
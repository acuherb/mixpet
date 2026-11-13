package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.LabTest;
import com.mix.model.vo.LabPanelSaveVo;
import com.mix.model.vo.LabPanelVo;

import java.util.List;

/**
 * 检验项字典 Service
 */
public interface ILabTestService extends IService<LabTest> {
    /** 字典分页（isPanel: 1套组 0单项 null不限） */
    IPage<LabTest> selectByPage(String testName, String testGroup, Integer isPanel, int numPage, int pageSize);

    /** 删除：被历史化验单引用时改为停用 */
    boolean deleteById(Integer id);

    /** 套组列表（含成员单项），用于开单选择 */
    List<LabPanelVo> listPanelsWithMembers();

    /** 新增套组（含成员） */
    boolean savePanel(LabPanelSaveVo vo);

    /** 修改套组（含成员，整体替换） */
    boolean updatePanel(LabPanelSaveVo vo);

    /** 删除套组（同步删成员；被历史化验单引用时套组头改为停用） */
    boolean deletePanel(Integer id);
}
package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.CheckupRecord;
import com.mix.model.entity.CheckupTemplate;
import com.mix.model.vo.CheckupSaveVo;

import java.util.List;

/**
 * 结构化体检表：模板 CRUD + 体检记录保存/查询。
 * 顾客只读自己宠物的报告，归属校验统一放在 Service，避免各接口漏判。
 */
public interface ICheckupService extends IService<CheckupTemplate> {

    /** 启用中的模板（接诊下拉） */
    List<CheckupTemplate> listActive();

    /** 管理端模板分页 */
    IPage<CheckupTemplate> selectTemplatePage(String seaName, int numPage, int pageSize);

    /** 保存/修改一次体检，返回 checkupId */
    Integer saveCheckup(CheckupSaveVo vo);

    /** 单份报告（顾客只能读自己宠物的；无权返回 null） */
    CheckupRecord getRecordForReader(Integer checkupId);

    /** 某宠物的历史报告（顾客只能读自己的宠物） */
    List<CheckupRecord> listByPetForReader(Integer petId);

    /** 某次就诊关联的体检报告（仅员工） */
    List<CheckupRecord> listByRecord(Integer recordId);

    /** 逻辑删除一份体检报告 */
    boolean removeCheckup(Integer checkupId);
}

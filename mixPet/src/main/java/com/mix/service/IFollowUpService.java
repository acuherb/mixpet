package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.FollowUpDto;
import com.mix.model.entity.FollowUp;

import java.util.List;

/**
 * 随访记录 Service
 */
public interface IFollowUpService extends IService<FollowUp> {
    /** 创建随访计划（自动带宠物/客户，状态待随访） */
    Result add(FollowUp followUp);

    /** 某病历的随访记录（按计划时间倒序） */
    List<FollowUp> listByRecord(Integer recordId);

    /** 随访管理分页（待随访/已随访 + 宠物或客户名搜索） */
    IPage<FollowUpDto> selectPage(Boolean status, String seaName, int numPage, int pageSize);

    /** 完成随访：记录结果/时间/随访人 */
    Result done(Integer followId, String result);
}

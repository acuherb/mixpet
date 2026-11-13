package com.mix.controller;

import com.mix.model.dto.RecordDto;
import com.mix.model.entity.Client;
import com.mix.model.vo.QuarantineCertVo;
import com.mix.util.UserUtil;
import com.mix.service.IMedicalRecordService;
import com.mix.service.IQuarantineCertService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检疫单：开具（含清单快照）/ 补打取数。
 * 版式移植自 PetSystem ReportPris_jianyi.mrt（A5 横向）。
 */
@RestController
@RequestMapping("/quarantine")
public class QuarantineController {

    @Resource
    private IQuarantineCertService quarantineCertService;
    @Resource
    private IMedicalRecordService recordService;

    /** 开具草稿：病历+宠物+客户+医生+处方明细预填；已有单据则返回该单 */
    @GetMapping("/draft/{recordId}")
    public QuarantineCertVo draft(@PathVariable Integer recordId) {
        checkOwn(recordId);
        return quarantineCertService.draft(recordId);
    }

    /** 开具并保存（生成 JY-YYYYMMDD-XXXX） */
    @PostMapping("/issue")
    public QuarantineCertVo issue(@RequestBody QuarantineCertVo vo) {
        if (vo != null && vo.getRecordId() != null) checkOwn(vo.getRecordId());
        return quarantineCertService.issue(vo);
    }

    /** 按单号取（补打） */
    @GetMapping("/{certNo}")
    public QuarantineCertVo selectByNo(@PathVariable String certNo) {
        QuarantineCertVo vo = quarantineCertService.selectByNo(certNo);
        // 顾客只能看自己宠物的单据
        if (UserUtil.getUser() instanceof Client client) {
            if (vo.getRecordId() != null) checkOwn(vo.getRecordId());
        }
        return vo;
    }

    /** 顾客侧归属校验：只能操作自己宠物的病历 */
    private void checkOwn(Integer recordId) {
        if (!(UserUtil.getUser() instanceof Client client)) return;
        RecordDto dto = recordService.selectRecordOne(recordId);
        if (dto == null || !client.getClientId().equals(dto.getClientId())) {
            throw new com.mix.common.BizException("无权操作该病历");
        }
    }
}

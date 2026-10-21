package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.HospitalAdviceMapper;
import com.mix.model.entity.HospitalAdvice;
import com.mix.model.entity.Doctor;
import com.mix.service.IHospitalAdviceService;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 住院医嘱单服务实现
 */
@Service
public class HospitalAdviceServiceImpl extends ServiceImpl<HospitalAdviceMapper, HospitalAdvice> implements IHospitalAdviceService {

    @Override
    @Transactional
    public boolean addAdvice(HospitalAdvice advice) {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            advice.setDoctorId(doctor.getDoctorId());
            advice.setDoctorName(doctor.getDoctorName());
        }
        if (advice.getStatus() == null) advice.setStatus(1);
        return save(advice);
    }

    @Override
    @Transactional
    public boolean addBatch(List<HospitalAdvice> list) {
        if (list == null || list.isEmpty()) return false;
        if (UserUtil.getUser() instanceof Doctor doctor) {
            Integer did = doctor.getDoctorId();
            String dname = doctor.getDoctorName();
            list.forEach(a -> {
                a.setDoctorId(did);
                a.setDoctorName(dname);
                if (a.getStatus() == null) a.setStatus(1);
            });
        }
        return saveBatch(list);
    }

    @Override
    public String stop(Integer adviceId, String reason) {
        if (StrUtil.isWhite(reason)) return "请填写停嘱原因（医疗留痕）";
        HospitalAdvice db = getById(adviceId);
        if (db == null) return "医嘱不存在";
        if (db.getStatus() != null && (db.getStatus() == 3 || db.getStatus() == 4))
            return "该医嘱已停嘱/已完成，不能重复操作";
        String operator = "系统";
        if (UserUtil.getUser() instanceof Doctor d) operator = d.getDoctorName();
        lambdaUpdate()
                .eq(HospitalAdvice::getAdviceId, adviceId)
                .set(HospitalAdvice::getStatus, 3)
                .set(HospitalAdvice::getStopReason, reason)
                .set(HospitalAdvice::getStopBy, operator)
                .set(HospitalAdvice::getStopTime, java.time.LocalDateTime.now())
                .update();
        return null;
    }

    @Override
    public String done(Integer adviceId) {
        HospitalAdvice db = getById(adviceId);
        if (db == null) return "医嘱不存在";
        if (db.getStatus() != null && db.getStatus() == 3) return "已停嘱的医嘱不能标记完成";
        lambdaUpdate().eq(HospitalAdvice::getAdviceId, adviceId)
                .set(HospitalAdvice::getStatus, 4).update();
        return null;
    }

    @Override
    public List<HospitalAdvice> listByHospital(Integer hospitalId) {
        return lambdaQuery()
                .eq(HospitalAdvice::getHospitalId, hospitalId)
                .eq(HospitalAdvice::getIsDel, 0)
                .orderByDesc(HospitalAdvice::getAdviceId)
                .list();
    }

    @Override
    public List<HospitalAdvice> listByRecord(Integer recordId) {
        return lambdaQuery()
                .eq(HospitalAdvice::getRecordId, recordId)
                .eq(HospitalAdvice::getIsDel, 0)
                .orderByDesc(HospitalAdvice::getAdviceId)
                .list();
    }
}

package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.mapper.CheckupRecordMapper;
import com.mix.mapper.CheckupTemplateMapper;
import com.mix.mapper.PetMapper;
import com.mix.model.entity.CheckupRecord;
import com.mix.model.entity.CheckupTemplate;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Pet;
import com.mix.model.vo.CheckupSaveVo;
import com.mix.service.ICheckupService;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CheckupServiceImpl extends ServiceImpl<CheckupTemplateMapper, CheckupTemplate>
        implements ICheckupService {

    private final CheckupRecordMapper recordMapper;
    private final PetMapper petMapper;

    public CheckupServiceImpl(CheckupRecordMapper recordMapper, PetMapper petMapper) {
        this.recordMapper = recordMapper;
        this.petMapper = petMapper;
    }

    @Override
    public List<CheckupTemplate> listActive() {
        return this.lambdaQuery()
                .eq(CheckupTemplate::getIsDel, 0)
                .eq(CheckupTemplate::getActive, true)
                .orderByAsc(CheckupTemplate::getSortOrder)
                .orderByAsc(CheckupTemplate::getTemplateId)
                .list();
    }

    @Override
    public IPage<CheckupTemplate> selectTemplatePage(String seaName, int numPage, int pageSize) {
        boolean hasName = seaName != null && !seaName.isBlank();
        return this.lambdaQuery()
                .eq(CheckupTemplate::getIsDel, 0)
                .like(hasName, CheckupTemplate::getName, seaName)
                .orderByAsc(CheckupTemplate::getSortOrder)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public Integer saveCheckup(CheckupSaveVo vo) {
        if (vo == null || vo.templateId() == null || vo.petId() == null) {
            throw new BizException("缺少体检模板或宠物");
        }
        CheckupTemplate tpl = this.getById(vo.templateId());
        if (tpl == null || (tpl.getIsDel() != null && tpl.getIsDel() != 0)) {
            throw new BizException("体检模板不存在");
        }
        Pet pet = petMapper.selectById(vo.petId());
        if (pet == null) throw new BizException("宠物不存在");

        CheckupRecord r = new CheckupRecord();
        r.setCheckupId(vo.checkupId());
        r.setTemplateId(tpl.getTemplateId());
        r.setTplKey(tpl.getTplKey());
        r.setTemplateName(tpl.getName());
        // schema 用前端传来的快照；缺失时回落模板当前内容
        r.setSchemaJson(StrUtil.isWhite(vo.schemaJson()) ? tpl.getSchemaJson() : vo.schemaJson());
        r.setResultsJson(vo.resultsJson());
        r.setMetaJson(vo.metaJson());
        r.setConclusion(vo.conclusion());
        r.setRecordId(vo.recordId());
        r.setPetId(pet.getPetId());
        // 归属以库中宠物为准，不信任前端传参
        r.setClientId(pet.getClientId());
        r.setIsDel(0L);
        if (UserUtil.getUser() instanceof Doctor d) {
            r.setDoctorId(d.getDoctorId());
            r.setCreateBy(d.getDoctorName());
        }

        if (vo.checkupId() == null) {
            r.setCheckTime(LocalDateTime.now());
            r.setCreateTime(LocalDateTime.now());
            recordMapper.insert(r);
        } else {
            recordMapper.updateById(r);
        }
        return r.getCheckupId();
    }

    @Override
    public CheckupRecord getRecordForReader(Integer checkupId) {
        if (checkupId == null) return null;
        CheckupRecord r = recordMapper.selectById(checkupId);
        if (r == null || (r.getIsDel() != null && r.getIsDel() != 0)) return null;
        return readable(r) ? r : null;
    }

    @Override
    public List<CheckupRecord> listByPetForReader(Integer petId) {
        if (petId == null) return List.of();
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return List.of();
        if (UserUtil.getUser() instanceof Client c
                && !c.getClientId().equals(pet.getClientId())) return List.of();
        return recordMapper.selectList(Wrappers.<CheckupRecord>lambdaQuery()
                .eq(CheckupRecord::getPetId, petId)
                .eq(CheckupRecord::getIsDel, 0)
                .orderByDesc(CheckupRecord::getCheckTime)
                // 迁移后历史数据可能很多，列表封顶
                .last("LIMIT 100"));
    }

    @Override
    public List<CheckupRecord> listByRecord(Integer recordId) {
        if (recordId == null) return List.of();
        return recordMapper.selectList(Wrappers.<CheckupRecord>lambdaQuery()
                .eq(CheckupRecord::getRecordId, recordId)
                .eq(CheckupRecord::getIsDel, 0)
                .orderByDesc(CheckupRecord::getCheckTime));
    }

    @Override
    public boolean removeCheckup(Integer checkupId) {
        if (checkupId == null) return false;
        // 项目开启了 MP 全局逻辑删除（is_del）。必须用 deleteById 让 MP 生成
        // UPDATE ... SET is_del=1；若手动 updateById 设 isDel，逻辑删除字段不进 SET，
        // 会生成「UPDATE checkup_record  WHERE ...」空 SET 的非法 SQL（曾报 BadSqlGrammarException）。
        return recordMapper.deleteById(checkupId) > 0;
    }

    /** 顾客只能读自己名下宠物的报告；员工不限制 */
    private boolean readable(CheckupRecord r) {
        if (UserUtil.getUser() instanceof Client c) {
            return c.getClientId() != null && c.getClientId().equals(r.getClientId());
        }
        return true;
    }
}

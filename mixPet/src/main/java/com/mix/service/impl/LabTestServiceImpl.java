package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.mapper.LabOrderItemMapper;
import com.mix.mapper.LabPanelMemberMapper;
import com.mix.mapper.LabTestMapper;
import com.mix.model.entity.LabOrderItem;
import com.mix.model.entity.LabPanelMember;
import com.mix.model.entity.LabTest;
import com.mix.model.vo.LabPanelSaveVo;
import com.mix.model.vo.LabPanelVo;
import com.mix.service.ILabTestService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 检验项字典 Service 实现
 */
@Service
public class LabTestServiceImpl extends ServiceImpl<LabTestMapper, LabTest> implements ILabTestService {
    private final LabOrderItemMapper labOrderItemMapper;
    private final LabPanelMemberMapper panelMemberMapper;

    @Autowired
    public LabTestServiceImpl(LabOrderItemMapper labOrderItemMapper, LabPanelMemberMapper panelMemberMapper) {
        this.labOrderItemMapper = labOrderItemMapper;
        this.panelMemberMapper = panelMemberMapper;
    }

    @Override
    public IPage<LabTest> selectByPage(String testName, String testGroup, Integer isPanel, int numPage, int pageSize) {
        boolean hasName = testName != null && !testName.isBlank();
        boolean hasGroup = testGroup != null && !testGroup.isBlank();
        var query = this.lambdaQuery()
                .like(hasName, LabTest::getTestName, testName)
                .eq(hasGroup, LabTest::getTestGroup, testGroup);
        // isPanel 为 0=单项 / 1=套组 / null=全部；仅在非 null 时追加条件，
        // 且把比较结果(isPanel==1)放进 eq 的值位，避免对 null 拆箱 NPE
        if (isPanel != null) {
            query = query.eq(LabTest::getIsPanel, isPanel == 1);
        }
        return query.orderByAsc(LabTest::getTestGroup)
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

    @Override
    public List<LabPanelVo> listPanelsWithMembers() {
        List<LabTest> panels = lambdaQuery()
                .eq(LabTest::getIsPanel, true)
                .eq(LabTest::getActive, true)
                .orderByAsc(LabTest::getTestGroup)
                .orderByDesc(LabTest::getCreateTime)
                .list();
        List<LabPanelVo> res = new ArrayList<>();
        for (LabTest p : panels) {
            LabPanelVo vo = new LabPanelVo();
            vo.setPanel(p);
            List<LabPanelMember> members = panelMemberMapper.selectList(Wrappers.<LabPanelMember>lambdaQuery()
                    .eq(LabPanelMember::getPanelId, p.getTestId())
                    .orderByAsc(LabPanelMember::getSortOrder));
            List<LabTest> mtests = members.stream()
                    .map(m -> getById(m.getMemberTestId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            vo.setMembers(mtests);
            res.add(vo);
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePanel(LabPanelSaveVo vo) {
        LabTest panel = vo.getPanel();
        if (panel == null || StrUtil.isWhite(panel.getTestName()))
            throw new BizException("套组名称不能为空");
        if (panel.getPanelPrice() == null || panel.getPanelPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new BizException("套组开单价不能为负数");
        panel.setIsPanel(true);
        if (panel.getActive() == null) panel.setActive(true);
        this.save(panel);
        saveMembers(panel.getTestId(), vo.getMemberIds());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePanel(LabPanelSaveVo vo) {
        LabTest panel = vo.getPanel();
        if (panel == null || panel.getTestId() == null) throw new BizException("缺少套组id");
        if (StrUtil.isWhite(panel.getTestName())) throw new BizException("套组名称不能为空");
        if (panel.getPanelPrice() == null || panel.getPanelPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new BizException("套组开单价不能为负数");
        panel.setIsPanel(true);
        this.updateById(panel);
        // 整体替换成员：先删后插
        panelMemberMapper.delete(Wrappers.<LabPanelMember>lambdaQuery().eq(LabPanelMember::getPanelId, panel.getTestId()));
        saveMembers(panel.getTestId(), vo.getMemberIds());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePanel(Integer id) {
        panelMemberMapper.delete(Wrappers.<LabPanelMember>lambdaQuery().eq(LabPanelMember::getPanelId, id));
        return this.deleteById(id);
    }

    /** 写入套组成员（按列表顺序即 sort_order），跳过不存在/非单项的 id */
    private void saveMembers(Integer panelId, List<Integer> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;
        int sort = 0;
        for (Integer mid : memberIds) {
            LabTest t = getById(mid);
            if (t == null || Boolean.TRUE.equals(t.getIsPanel()) || Boolean.FALSE.equals(t.getActive())) continue;
            LabPanelMember m = new LabPanelMember();
            m.setPanelId(panelId);
            m.setMemberTestId(mid);
            m.setSortOrder(sort++);
            panelMemberMapper.insert(m);
        }
    }
}
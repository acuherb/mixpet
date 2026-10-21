package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.ClientCountCard;
import com.mix.model.entity.CountCardTemplate;
import com.mix.model.entity.MemberLevel;
import com.mix.security.model.RoleConst;
import com.mix.service.IMemberService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员卡 / 次卡 / 积分
 */
@RestController
@RequestMapping("/member")
public class MemberController {
    private final IMemberService memberService;

    @Autowired
    public MemberController(IMemberService memberService) {
        this.memberService = memberService;
    }

    // ===== 会员等级 =====
    @GetMapping("/level")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public List<MemberLevel> levels() {
        return memberService.listActive();
    }

    @PostMapping("/level")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result saveLevel(@RequestBody MemberLevel level) {
        if (StrUtil.isWhite(level.getLevelName())) return Result.error("等级名称不能为空");
        if (level.getLevelKey() == null) return Result.error("等级键不能为空");
        if (level.getActive() == null) level.setActive(true);
        return Result.choice("添加", memberService.save(level));
    }

    @PutMapping("/level")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result updateLevel(@RequestBody MemberLevel level) {
        if (level.getLevelId() == null) return Result.error("缺少等级id");
        return Result.choice("修改", memberService.updateById(level));
    }

    @DeleteMapping("/level/{id}")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result delLevel(@PathVariable Integer id) {
        return Result.choice("删除", memberService.removeById(id));
    }

    // ===== 次卡模板 =====
    @GetMapping("/cardTemplate")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public List<CountCardTemplate> cardTemplates() {
        return memberService.listCardTemplates();
    }

    @PostMapping("/cardTemplate")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result saveCardTemplate(@RequestBody CountCardTemplate t) {
        return memberService.saveCardTemplate(t);
    }

    @DeleteMapping("/cardTemplate/{id}")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result delCardTemplate(@PathVariable Integer id) {
        return memberService.delCardTemplate(id);
    }

    // ===== 客户会员操作 =====
    @GetMapping("/cards/{clientId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<ClientCountCard> cards(@PathVariable Integer clientId) {
        return memberService.clientCards(clientId);
    }

    /** 售卡：扣余额生成次卡 */
    @PostMapping("/buyCard")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result buyCard(Integer clientId, Integer templateId) {
        return memberService.buyCard(clientId, templateId);
    }

    /** 核销次卡一次 */
    @PostMapping("/useCard/{cardId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result useCard(@PathVariable Integer cardId) {
        return memberService.useCard(cardId);
    }

    /** 积分兑换余额（100分=1元） */
    @PostMapping("/exchange")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result exchange(Integer clientId, Integer points) {
        return memberService.exchangePoints(clientId, points);
    }
}

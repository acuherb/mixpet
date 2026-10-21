package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.entity.ClientCountCard;
import com.mix.model.entity.CountCardTemplate;
import com.mix.model.entity.MemberLevel;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员/次卡/积分 Service
 */
public interface IMemberService extends IService<MemberLevel> {
    List<MemberLevel> listActive();

    /** 某客户等级对应的会员等级（无配置时返回 null，调用方用默认值） */
    MemberLevel levelOf(Integer clientLevel);

    /** 充值返赠金额 */
    BigDecimal rechargeBonus(Integer clientLevel, BigDecimal money);

    /** 消费积分率 */
    BigDecimal pointsRate(Integer clientLevel);

    /** 某客户的会员折扣率（1=不打折；无等级配置或异常值返回1） */
    BigDecimal discountOf(Integer clientId);

    /** 购卡：扣余额生成次卡 */
    Result buyCard(Integer clientId, Integer templateId);

    /** 核销次卡一次 */
    Result useCard(Integer cardId);

    /** 某客户的次卡 */
    List<ClientCountCard> clientCards(Integer clientId);

    /** 积分兑换余额（100分=1元） */
    Result exchangePoints(Integer clientId, Integer points);

    /** 支付后累积积分 */
    void earnPoints(Integer clientId, BigDecimal amount);

    /** 退款后按同等金额冲销积分（不足时不扣为负） */
    void deductPoints(Integer clientId, BigDecimal amount);

    /** 次卡模板列表 */
    List<CountCardTemplate> listCardTemplates();

    /** 新增/修改次卡模板 */
    Result saveCardTemplate(CountCardTemplate t);

    /** 删除次卡模板 */
    Result delCardTemplate(Integer id);
}
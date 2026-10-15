package com.ship.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.ClientCountCardMapper;
import com.ship.mapper.CountCardTemplateMapper;
import com.ship.mapper.MemberLevelMapper;
import com.ship.model.entity.Client;
import com.ship.model.entity.ClientCountCard;
import com.ship.model.entity.CountCardTemplate;
import com.ship.model.entity.MemberLevel;
import com.ship.service.IClientService;
import com.ship.service.IMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements IMemberService {
    private final CountCardTemplateMapper cardTemplateMapper;
    private final ClientCountCardMapper clientCardMapper;
    private final IClientService clientService;

    @Autowired
    public MemberServiceImpl(CountCardTemplateMapper cardTemplateMapper, ClientCountCardMapper clientCardMapper,
                             IClientService clientService) {
        this.cardTemplateMapper = cardTemplateMapper;
        this.clientCardMapper = clientCardMapper;
        this.clientService = clientService;
    }

    @Override
    public List<MemberLevel> listActive() {
        return this.lambdaQuery().eq(MemberLevel::getActive, true)
                .orderByAsc(MemberLevel::getSort).orderByAsc(MemberLevel::getLevelKey).list();
    }

    @Override
    public MemberLevel levelOf(Integer clientLevel) {
        Integer key = clientLevel == null ? 0 : clientLevel;
        MemberLevel lv = this.lambdaQuery().eq(MemberLevel::getActive, true)
                .eq(MemberLevel::getLevelKey, key).one();
        if (lv == null && key != 0)
            lv = this.lambdaQuery().eq(MemberLevel::getActive, true).eq(MemberLevel::getLevelKey, 0).one();
        return lv;
    }

    @Override
    public BigDecimal rechargeBonus(Integer clientLevel, BigDecimal money) {
        MemberLevel lv = levelOf(clientLevel);
        if (lv == null || lv.getRechargeBonus() == null || money == null) return BigDecimal.ZERO;
        return money.multiply(lv.getRechargeBonus()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal pointsRate(Integer clientLevel) {
        MemberLevel lv = levelOf(clientLevel);
        if (lv == null || lv.getPointsRate() == null) return BigDecimal.ONE; // 默认1元=1分
        return lv.getPointsRate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result buyCard(Integer clientId, Integer templateId) {
        if (clientId == null || templateId == null) return Result.error("缺少参数");
        CountCardTemplate t = cardTemplateMapper.selectById(templateId);
        if (t == null || !Boolean.TRUE.equals(t.getActive())) return Result.error("次卡不存在或已下架");
        if (t.getTimes() == null || t.getTimes() <= 0) return Result.error("次卡次数配置错误");
        Client client = clientService.getById(clientId);
        if (client == null) return Result.error("客户不存在");
        BigDecimal price = t.getPrice() == null ? BigDecimal.ZERO : t.getPrice();
        BigDecimal balance = client.getClientBalance() == null ? BigDecimal.ZERO : client.getClientBalance();
        if (balance.compareTo(price) < 0) return Result.error("余额不足（当前 " + balance + " 元），请先充值");
        boolean deducted = clientService.lambdaUpdate()
                .eq(Client::getClientId, clientId)
                .ge(Client::getClientBalance, price)
                .setSql("client_balance = client_balance - " + price.toPlainString())
                .update();
        if (!deducted) return Result.error("余额不足，扣款失败");
        ClientCountCard card = new ClientCountCard();
        card.setClientId(clientId);
        card.setTemplateId(templateId);
        card.setName(t.getName());
        card.setRemainTimes(t.getTimes());
        card.setTotalTimes(t.getTimes());
        if (t.getValidityDays() != null && t.getValidityDays() > 0)
            card.setExpireDate(LocalDate.now().plusDays(t.getValidityDays()));
        card.setStatus(true);
        clientCardMapper.insert(card);
        return Result.success("购卡成功：「" + t.getName() + "」" + t.getTimes() + "次");
    }

    @Override
    public Result useCard(Integer cardId) {
        ClientCountCard card = clientCardMapper.selectById(cardId);
        if (card == null) return Result.error("次卡不存在");
        if (!Boolean.TRUE.equals(card.getStatus())) return Result.error("该次卡已用完或已作废");
        if (card.getExpireDate() != null && card.getExpireDate().isBefore(LocalDate.now()))
            return Result.error("该次卡已过期");
        if (card.getRemainTimes() == null || card.getRemainTimes() <= 0)
            return Result.error("该次卡已无剩余次数");
        int remain = card.getRemainTimes() - 1;
        clientCardMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<ClientCountCard>lambdaUpdate()
                .eq(ClientCountCard::getCardId, cardId)
                .set(ClientCountCard::getRemainTimes, remain)
                .set(ClientCountCard::getStatus, remain > 0));
        return Result.success("核销成功，剩余 " + remain + " 次");
    }

    @Override
    public List<ClientCountCard> clientCards(Integer clientId) {
        return clientCardMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<ClientCountCard>lambdaQuery()
                .eq(ClientCountCard::getClientId, clientId)
                .orderByDesc(ClientCountCard::getCardId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result exchangePoints(Integer clientId, Integer points) {
        if (clientId == null || points == null || points <= 0) return Result.error("兑换积分需大于0");
        if (points % 100 != 0) return Result.error("积分需为100的倍数（100分=1元）");
        Client client = clientService.getById(clientId);
        if (client == null) return Result.error("客户不存在");
        int own = client.getClientPoints() == null ? 0 : client.getClientPoints();
        if (own < points) return Result.error("积分不足（当前 " + own + " 分）");
        BigDecimal money = BigDecimal.valueOf(points).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        clientService.lambdaUpdate()
                .eq(Client::getClientId, clientId)
                .setSql("client_points = client_points - " + points)
                .setSql("client_balance = client_balance + " + money.toPlainString())
                .update();
        return Result.success("兑换成功：" + points + " 分 → ￥" + money.toPlainString());
    }

    @Override
    public void earnPoints(Integer clientId, BigDecimal amount) {
        if (clientId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Client client = clientService.getById(clientId);
        if (client == null) return;
        int pts = amount.multiply(pointsRate(client.getClientLevel())).setScale(0, RoundingMode.HALF_UP).intValue();
        if (pts <= 0) return;
        clientService.lambdaUpdate()
                .eq(Client::getClientId, clientId)
                .setSql("client_points = client_points + " + pts)
                .update();
    }

    @Override
    public List<CountCardTemplate> listCardTemplates() {
        return cardTemplateMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<CountCardTemplate>lambdaQuery()
                .orderByAsc(CountCardTemplate::getTemplateId));
    }

    @Override
    public Result saveCardTemplate(CountCardTemplate t) {
        if (t == null || t.getName() == null || t.getName().isBlank()) return Result.error("次卡名称不能为空");
        if (t.getTimes() == null || t.getTimes() <= 0) return Result.error("次数需大于0");
        if (t.getActive() == null) t.setActive(true);
        if (t.getTemplateId() == null) {
            cardTemplateMapper.insert(t);
            return Result.success("次卡已添加");
        }
        return Result.choice("修改", cardTemplateMapper.updateById(t) > 0);
    }

    @Override
    public Result delCardTemplate(Integer id) {
        return Result.choice("删除", cardTemplateMapper.deleteById(id) > 0);
    }
}

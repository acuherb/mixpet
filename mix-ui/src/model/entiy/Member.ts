/**
 * 会员等级/会员卡模板
 */
export interface MemberLevel {
    levelId?: number;
    /** 等级键：0普通 1银 2金 3钻石 */
    levelKey: number;
    levelName: string;
    /** 折扣率 1=不打折 */
    discount?: number | null;
    /** 充值返赠比例 0.1=充100送10 */
    rechargeBonus?: number | null;
    /** 消费积分率 1元=N分 */
    pointsRate?: number | null;
    sort?: number | null;
    active?: boolean | null;
}

/**
 * 次卡模板
 */
export interface CountCardTemplate {
    templateId?: number;
    name: string;
    serviceType?: string | null;
    times: number;
    price?: number | null;
    validityDays?: number | null;
    active?: boolean | null;
}

/**
 * 客户次卡
 */
export interface ClientCountCard {
    cardId: number;
    clientId: number;
    templateId: number;
    name: string | null;
    remainTimes: number | null;
    totalTimes: number | null;
    expireDate: string | null;
    status: boolean | null;
    createTime?: string | null;
}

/**
 * 会员等级类别折扣（按商品类别 drug_type，开方单价折算用，与等级折扣叠加）
 */
export interface MemberLevelDiscount {
    id?: number;
    levelId?: number;
    itemType: string;
    /** 类别折扣率 0-1，0.9=九折 */
    discount: number;
    active?: boolean | null;
}

/**
 * 会员储值流水（余额每次变动一条，前/后余额成对落库）
 */
export interface ClientBalanceLog {
    logId: number;
    clientId: number;
    /** RECHARGE充值 / PAY收款扣款 / REFUND退款回充 / BUY_CARD购卡 / POINTS_EXCHANGE积分兑换 / MIGRATION_OPEN迁移期初 */
    changeType: string;
    /** 变动金额（带符号） */
    amount: number;
    balanceBefore: number | null;
    balanceAfter: number | null;
    refType?: string | null;
    refId?: number | null;
    operator?: string | null;
    note?: string | null;
    createTime?: string | null;
}


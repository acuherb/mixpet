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

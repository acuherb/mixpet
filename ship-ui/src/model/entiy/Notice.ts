export interface Notice {
    noticeId: number;
    noticeTitle: string;
    noticeFile: string;
    createTime?: null | string;
    updateTime?: null | string;
    isDisable?: null | boolean;
    /** 公告类型：false顾客公告 true系统提醒(仅员工可见) */
    noticeType?: null | boolean;
}

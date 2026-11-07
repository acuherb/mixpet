import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";

/** 顾客定向站内通知（报告出具、结果修订等） */
export interface ClientNotification {
    notifyId: number;
    clientId: number;
    title: string;
    content: string;
    notifyType?: string | null;
    refType?: string | null;
    refId?: number | null;
    isRead: boolean;
    createTime?: string | null;
}

// 我的通知（最多 50 条，后端强制按登录顾客过滤）
export const reqNotifySelf = (): Promise<ClientNotification[]> =>
    reqApi<string, ClientNotification[]>("notify/self")
// 未读数（个人中心角标）
export const reqNotifyUnread = (): Promise<number> =>
    reqApi<string, number>("notify/unread")
// 单条标为已读
export const reqNotifyRead = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("notify/read/" + id, {})
// 全部标为已读
export const reqNotifyReadAll = (): Promise<Res> =>
    reqApi.put<string, Res>("notify/readAll", {})

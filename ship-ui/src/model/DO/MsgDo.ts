/**
 * 聊天记录
 */
export interface MsgDo {
    msgId?: number;
    isMine: boolean;
    msgContent: string;
    isImg: boolean;
    msgType?: string;
    msgName?: string;
    createTime: string;
    isRead: boolean;
    roleName: string;
    rolePhoto: string;
}
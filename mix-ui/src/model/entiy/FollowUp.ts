/**
 * 随访记录
 */
export interface FollowUp {
    followId: number;
    recordId: number;
    petId: number;
    clientId: number;
    followType: string | null;
    content: string | null;
    result: string | null;
    planTime: string | null;
    followTime: string | null;
    status: boolean | null;
    followBy: string | null;
    note: string | null;
    createTime?: string | null;
}

/** 随访记录联查（含宠物/客户/诊断） */
export interface FollowUpDto extends FollowUp {
    petName: string | null;
    clientName: string | null;
    diagnosis: string | null;
}

/** 随访方式选项 */
export const followTypes = ['电话', '微信', '到店', '上门']

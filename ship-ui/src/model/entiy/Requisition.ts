/**
 * 检验/影像申请单（国标，可打印）
 */
export interface ReqItem {
    name: string;
    group: string | null;
    refRange: string | null;
    unit: string | null;
}

export interface Requisition {
    type: string;
    orderId: number;
    orderNo: string;
    createTime: string;
    clientName: string;
    petName: string;
    petVariety: string | null;
    petSex: boolean | null;
    petBirthday: string | null;
    petWeight: number | null;
    petChip: string | null;
    diagnosis: string;
    doctorName: string;
    doctorCert: string | null;
    items: ReqItem[];
    examPart: string | null;
    remark: string | null;
    fee: number;
}

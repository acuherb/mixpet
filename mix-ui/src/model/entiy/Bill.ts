/**
 * 收费账单实体（对齐后端 Bill）
 */
export interface Bill {
    billId: number;
    clientId: number;
    petId: number | null;
    recordId: number;
    feeRegister: number | null;
    feeTreat: number | null;
    feeDrug: number | null;
    feeLab: number | null;
    feeHospital: number | null;
    feeImage: number | null;
    feePackage: number | null;
    feeSurgery: number | null;
    totalMoney: number | null;
    discount: number | null;
    discountMoney: number | null;
    /** 折后应付/实收 */
    realMoney: number | null;
    /** 0未付 1已付 */
    payStatus: boolean;
    /** 0余额 1现金 */
    payType: boolean | null;
    /** 0未发药 1已发药 */
    dispenseStatus: boolean;
    dispenseBy: string | null;
    dispenseTime: string | null;
    /** 0未退 1已退 */
    refundStatus: boolean;
    refundBy: string | null;
    refundTime: string | null;
    createTime: string | null;
    updateTime: string | null;
    isDel: number | null;
}

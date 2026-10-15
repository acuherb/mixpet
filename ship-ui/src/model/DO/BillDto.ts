/**
 * 收费列表行（联查）
 */
export interface BillDto {
    billId: number;
    clientId: number;
    petId: number;
    recordId: number | null;
    feeRegister: number;
    feeTreat: number;
    feeDrug: number;
    feeLab: number;
    feeHospital: number;
    feeImage: number;
    feePackage: number;
    feeSurgery: number;
    totalMoney: number;
    /** 会员折扣率快照（1=不打折） */
    discount?: number | null;
    /** 优惠金额 */
    discountMoney?: number | null;
    /** 折后应付/实收（旧单为空时按原价） */
    realMoney?: number | null;
    payStatus: boolean;
    payType: boolean | null;
    /** 发药状态 0未发 1已发 */
    dispenseStatus?: boolean;
    /** 发药核对人 */
    dispenseBy?: string | null;
    /** 退款状态 0未退 1已退 */
    refundStatus?: boolean;
    createTime: string | null;
    payTime: string | null;
    clientName: string;
    clientTel: string | null;
    petName: string;
    doctorName: string | null;
    clientBalance: number | null;
}

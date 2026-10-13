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
    totalMoney: number;
    payStatus: boolean;
    payType: boolean | null;
    createTime: string | null;
    payTime: string | null;
    clientName: string;
    clientTel: string | null;
    petName: string;
    doctorName: string | null;
    clientBalance: number | null;
}

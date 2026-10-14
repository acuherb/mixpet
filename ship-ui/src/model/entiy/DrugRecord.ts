/**
 * 药品出入库台账
 */
export interface DrugRecord {
    recordId: number;
    drugId: number;
    drugName: string | null;
    /** 1入库 2发药出库 3退款回库 4盘亏/报损 */
    changeType: number;
    quantity: number;
    beforeStock: number | null;
    afterStock: number | null;
    operator: string | null;
    note: string | null;
    createTime?: string | null;
}

/** 变动类型中文 */
export const changeTypeText = (t: number): string => {
    switch (t) {
        case 1:
            return '入库'
        case 2:
            return '发药出库'
        case 3:
            return '退款回库'
        case 4:
            return '盘亏/报损'
        default:
            return '其他'
    }
}

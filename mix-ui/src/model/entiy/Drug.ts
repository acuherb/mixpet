export interface Drug {
    drugId: number;
    drugName: string;
    drugSpec: string | null;
    drugUnit: string | null;
    drugType: string | null;
    drugPrice: number | null;
    /** 进货价(成本价)，用于药品毛利核算 */
    costPrice?: number | null;
    drugStock: number | null;
    /** 库存预警阈值：stock <= 该值即低库存 */
    warningStock?: number | null;
    /** 有效期至 */
    expireDate?: string | null;
    drugInfo: string | null;
    brand?: string | null;
    prevent?: string | null;
    intervalDays?: number | null;
    /** 每kg体重单次用量（单位同药剂单位），空=不自动算量 */
    dosePerKg?: number | null;
    /** 固定单次用量（与体重无关，优先），空=不自动算量 */
    doseFixed?: number | null;
    active: boolean;
}

/**
 * 空数据
 */
export const exampleDrug = (): Drug => {
    return {
        drugId: 0,
        drugName: "",
        drugSpec: "",
        drugUnit: "盒",
        drugType: "西药",
        drugPrice: 0,
        costPrice: null,
        drugStock: 0,
        warningStock: 0,
        expireDate: null,
        drugInfo: "",
        brand: null,
        prevent: null,
        intervalDays: null,
        dosePerKg: null,
        doseFixed: null,
        active: true
    }
}

/** 药品分类选项 */
export const drugTypes = ["西药", "中成药", "外用", "疫苗", "驱虫", "保健"]

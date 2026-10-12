export interface Drug {
    drugId: number;
    drugName: string;
    drugSpec: string | null;
    drugUnit: string | null;
    drugType: string | null;
    drugPrice: number | null;
    drugStock: number | null;
    drugInfo: string | null;
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
        drugStock: 0,
        drugInfo: "",
        active: true
    }
}

/** 药品分类选项 */
export const drugTypes = ["西药", "中成药", "外用", "疫苗", "驱虫", "保健"]

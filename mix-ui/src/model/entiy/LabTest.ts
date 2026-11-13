/**
 * 检验项目字典
 */
export interface LabTest {
    testId: number;
    testName: string;
    testGroup: string | null;
    testUnit: string | null;
    refRange: string | null;
    testPrice: number | null;
    /** 是否套组：true=套组(只收一次费) false=单项 */
    isPanel: boolean;
    /** 套组开单价(元) */
    panelPrice: number | null;
    /** 套组进价/试剂盘成本(元) */
    panelCost: number | null;
    active: boolean;
}

/**
 * 空数据
 */
export const exampleLabTest = (): LabTest => {
    return {
        testId: 0,
        testName: "",
        testGroup: "血常规",
        testUnit: "",
        refRange: "",
        testPrice: 0,
        isPanel: false,
        panelPrice: 0,
        panelCost: 0,
        active: true
    }
}

/** 检验项分组选项 */
export const labTestGroups = ["血常规", "生化", "尿检", "镜检", "粪便", "免疫"]
/**
 * 影像检查项目字典
 */
export interface ImageTest {
    itemId: number;
    itemName: string;
    groupName: string;
    price: number;
    detail: string;
    /** 报告模板项(JSON数组字符串，如 ["检查体位","影像所见"]) */
    reportTpl?: string | null;
}

export const exampleImageTest = (): ImageTest => ({
    itemId: 0,
    itemName: '',
    groupName: '',
    price: 0,
    detail: '',
    reportTpl: ''
})

export const imageGroups = ['X光', 'B超', 'CT']
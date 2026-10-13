/**
 * 服务套餐（驱虫/体检/免疫套餐等）
 */
export interface ServicePackage {
    packageId: number;
    packageName: string;
    category: string | null;
    price: number;
    content: string | null;
    active: boolean;
}

export const examplePackage = (): ServicePackage => ({
    packageId: 0,
    packageName: '',
    category: '体检',
    price: 0,
    content: '',
    active: true
})

/** 套餐分类选项 */
export const packageCategories = ['体检', '驱虫', '免疫', '美容', '其他']

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
    /** 包含的检查项目（检验/影像） */
    items?: PackageItem[];
}

/** 套餐包含项目（快照） */
export interface PackageItem {
    id?: number;
    packageId?: number;
    /** lab 检验 / image 影像 */
    itemType: 'lab' | 'image';
    itemId: number;
    itemName: string;
    itemPrice?: number | null;
}

export const examplePackage = (): ServicePackage => ({
    packageId: 0,
    packageName: '',
    category: '体检',
    price: 0,
    content: '',
    active: true,
    items: []
})

/** 套餐分类选项 */
export const packageCategories = ['体检', '驱虫', '免疫', '美容', '其他']

/**
 * 就诊选购的套餐记录（快照）
 */
export interface RecordPackage {
    id: number;
    recordId: number;
    packageId: number | null;
    packageName: string;
    price: number;
}

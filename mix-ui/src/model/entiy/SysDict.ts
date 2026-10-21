/**
 * 通用字典/系统设置项
 */
export interface SysDict {
    dictId: number;
    dictType: string;
    dictKey: string;
    sort: number | null;
    active: boolean | null;
    createTime?: string | null;
}

/** 字典类型 → 中文名（设置中心左侧菜单） */
export const dictTypes: { type: string; label: string }[] = [
    {type: 'lab_group', label: '化验分组'},
    {type: 'img_group', label: '影像分组'},
    {type: 'drug_type', label: '药品分类'},
    {type: 'drug_unit', label: '药品单位'},
    {type: 'surgery_type', label: '手术类型'},
    {type: 'anesthesia_type', label: '麻醉方式'},
]

export const dictLabel = (type: string): string =>
    dictTypes.find(d => d.type === type)?.label || type

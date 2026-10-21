/**
 * 权限等级（auth_lv）说明：固定 5 档，驱动 Spring Security 角色，与"职称(job)"解耦。
 * 高档位累计继承低档位角色（院长=8 拥有全部）。
 */
export interface PermItem {
    lv: number;
    name: string;
    desc: string;
}

export const PERMISSIONS: PermItem[] = [
    {lv: 8, name: '超级管理员', desc: '全院最高权限：所有功能，含系统与权限管理'},
    {lv: 6, name: '管理员', desc: '后台运营：客户/宠物/医生/科室/职务/药品/账单/各字典管理'},
    {lv: 4, name: '医生', desc: '接诊开方、开化验/影像单、入院登记'},
    {lv: 2, name: '医护', desc: '挂号、收费发药、充值、录入化验/影像结果、出院结算'},
    {lv: 0, name: '无权限', desc: '仅可登录，无操作权限（临时/禁用）'},
]

/** 权限等级 -> 权限名称 */
export const permName = (lv: number): string =>
    PERMISSIONS.find(p => p.lv === lv)?.name ?? '未知权限'

/** 权限等级 -> 能干什么的说明 */
export const permDesc = (lv: number | null | undefined): string =>
    PERMISSIONS.find(p => p.lv === lv)?.desc ?? ''

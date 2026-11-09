import {ref} from 'vue'
import {reqGetLoginD} from '@/request/DoctorApi'

/**
 * 权限等级（auth_lv）说明：固定 5 档，驱动 Spring Security 角色，与"职称(job)"解耦。
 * 角色不再简单累加：NURSE 是「在职员工基线」(>=2 都有)，DOCTOR 是「临床开方资格」(仅 4 档)，
 * 6/8 是纯管理账号、不具备临床资格——要看病请另用医生账号登录（用户 2026-11-09 定的规则）。
 */
export interface PermItem {
    lv: number;
    name: string;
    desc: string;
}

export const PERMISSIONS: PermItem[] = [
    {lv: 8, name: '超级管理员', desc: '全院最高管理权限：系统/权限/组织/经营配置；不含临床开方与写病历'},
    {lv: 6, name: '管理员', desc: '后台运营：客户/宠物/药品/账单/排班/字典管理；不含临床开方与写病历'},
    {lv: 4, name: '医生', desc: '接诊开方、写病历、开化验/影像单、入院登记'},
    {lv: 2, name: '医护', desc: '挂号、收费发药、充值、录入化验/影像结果、出院结算'},
    {lv: 0, name: '无权限', desc: '仅可登录，无操作权限（临时/禁用）'},
]

/** 权限等级 -> 权限名称 */
export const permName = (lv: number): string =>
    PERMISSIONS.find(p => p.lv === lv)?.name ?? '未知权限'

/** 权限等级 -> 能干什么的说明 */
export const permDesc = (lv: number | null | undefined): string =>
    PERMISSIONS.find(p => p.lv === lv)?.desc ?? ''

/** 各页面共用：拉一次当前登录员工的权限档，异步回来后按钮可见性自动更新 */
export const useAuthLv = () => {
    const authLv = ref(0)
    reqGetLoginD().then(d => { authLv.value = Number(d?.authLv ?? 0) }).catch(() => {})
    return authLv
}

/** 可过 @Secured(MANAGER) / ADMIN+MANAGER 门槛（管理账号） */
export const canManage = (lv: number | null | undefined): boolean => Number(lv ?? 0) >= 6

/** 可过纯 @Secured(DOCTOR) 门槛；管理账号现在不带 DOCTOR，只有 4 档可以 */
export const canClinicallyEdit = (lv: number | null | undefined): boolean => Number(lv ?? 0) === 4

/** 需要 DOCTOR 资格（后端纯 @Secured(DOCTOR)）的临床写操作入口 */
const CLINICAL_WRITE = new Set([
    '/doctorPage/d-appoint', '/doctorPage/d-lab', '/doctorPage/d-image', '/doctorPage/d-rx',
])

/** 临床/前台作业入口：管理账号只做管理，不展示这些 */
const CLINICAL_WORK = new Set([
    '/doctorPage/d-appoint', '/doctorPage/d-lab', '/doctorPage/d-image', '/doctorPage/d-rx',
    '/doctorPage/d-chat', '/doctorPage/z-examResult', '/doctorPage/z-follow',
    '/doctorPage/z-remind', '/doctorPage/z-immune', '/doctorPage/z-hospital',
    '/doctorPage/z-surgery', '/doctorPage/y-billList',
])

/**
 * 入口是否对该权限档可见。后端 @Secured 才是真拦截，这里只避免「看得到、点不动」。
 * 管理账号(>=6) 隐藏全部临床作业，保留查询/档案/打印/配置；
 * 医护(<4) 隐藏开单/开方；0 档不展示任何业务入口。
 */
export const canOpenAction = (path: string, authLv: number | null | undefined): boolean => {
    const p = String(path || '').split('?')[0]
    const lv = Number(authLv ?? 0)
    if (lv <= 0) return false
    if (lv >= 6) return !CLINICAL_WORK.has(p)
    if (lv < 4 && CLINICAL_WRITE.has(p)) return false
    return true
}

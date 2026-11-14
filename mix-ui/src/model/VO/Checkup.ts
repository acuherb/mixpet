/** 结构化体检表：模板（可编辑树）与报告（结果 + 快照） */

export type CheckupItemType = 'check' | 'choice' | 'number' | 'text' | 'textarea' | 'note'
export type CheckupSide = 'lr' | 'four'

export interface CheckupItem {
    key: string;
    label: string;
    type: CheckupItemType;
    /** check/choice 可分左右或四肢记录 */
    side?: CheckupSide;
    /** choice 的选项 */
    options?: string[];
    /** number 的单位 */
    unit?: string;
    /** 标记为「正常」默认项，接诊时可一键置正常 */
    normal?: boolean;
}

export interface CheckupSection {
    key: string;
    title: string;
    items: CheckupItem[];
}

export interface CheckupSchema {
    title: string;
    sections: CheckupSection[];
}

export interface CheckupTemplate {
    templateId: number;
    tplKey: string;
    name: string;
    category: string;
    description: string;
    schemaJson: string;
    active: boolean;
    isBuiltin: boolean;
    sortOrder: number;
}

/** 表头快照：打印时自包含，管理员改模板/宠物改名都不影响历史报告 */
export interface CheckupMeta {
    hospitalName?: string;
    caseNo?: string;
    checkupNo?: string;
    owner?: string;
    clientNo?: string;
    species?: string;
    age?: string;
    sex?: string;
    petNo?: string;
    petName?: string;
    doctor?: string;
    date?: string;
    weight?: string;
}

export interface CheckupRecord {
    checkupId: number;
    templateId: number;
    tplKey: string;
    templateName: string;
    schemaJson: string;
    resultsJson: string;
    metaJson: string;
    conclusion: string;
    recordId?: number | null;
    petId: number;
    clientId?: number | null;
    doctorId?: number | null;
    checkTime: string;
    createBy?: string;
}

export interface CheckupSaveVo {
    checkupId?: number;
    templateId: number;
    petId: number;
    recordId?: number;
    schemaJson: string;
    resultsJson: string;
    metaJson: string;
    conclusion: string;
}

export type CheckupResults = Record<string, any>

export const emptySchema = (): CheckupSchema => ({title: '新体检表', sections: []})

export const parseSchema = (s?: string | null): CheckupSchema => {
    if (!s) return emptySchema()
    try {
        const v = JSON.parse(s)
        return v && Array.isArray(v.sections) ? v as CheckupSchema : emptySchema()
    } catch {
        return emptySchema()
    }
}

export const parseResults = (s?: string | null): CheckupResults => {
    if (!s) return {}
    try {
        const v = JSON.parse(s)
        return v && typeof v === 'object' ? v as CheckupResults : {}
    } catch {
        return {}
    }
}

export const parseMeta = (s?: string | null): CheckupMeta => {
    if (!s) return {}
    try {
        const v = JSON.parse(s)
        return v && typeof v === 'object' ? v as CheckupMeta : {}
    } catch {
        return {}
    }
}

/** 按 item 类型生成一份空结果 */
export const emptyItemValue = (it: CheckupItem): any => {
    if (it.type === 'check') {
        if (it.side === 'lr') return {l: false, r: false}
        if (it.side === 'four') return {lf: false, rf: false, lh: false, rh: false}
        return false
    }
    if (it.type === 'choice') {
        if (it.side === 'lr') return {l: '', r: ''}
        return ''
    }
    return ''
}

/** 一份完整空结果（note 不产生结果） */
export const emptyResults = (schema: CheckupSchema): CheckupResults => {
    const out: CheckupResults = {}
    for (const sec of schema.sections || []) {
        for (const it of sec.items || []) {
            if (it.type !== 'note') out[it.key] = emptyItemValue(it)
        }
    }
    return out
}

/** 一键「全部正常」：每个分区里标记 normal 的项置真，其余清空 */
export const normalResults = (schema: CheckupSchema): CheckupResults => {
    const out = emptyResults(schema)
    for (const sec of schema.sections || []) {
        const normal = (sec.items || []).find(i => i.normal)
        if (normal && normal.type === 'check') {
            out[normal.key] = normal.side === 'lr' ? {l: true, r: true}
                : normal.side === 'four' ? {lf: true, rf: true, lh: true, rh: true} : true
        } else if (normal && normal.type === 'choice' && normal.options?.length) {
            out[normal.key] = normal.side === 'lr' ? {l: normal.options[0], r: normal.options[0]} : normal.options[0]
        }
    }
    return out
}

/** 新建分区/项目时生成不重复 key */
export const nextKey = (prefix: string, used: Set<string>): string => {
    let i = 1
    while (used.has(`${prefix}${i}`)) i++
    return `${prefix}${i}`
}

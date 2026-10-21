/**
 * 通用 CSV 导出（前端，导出当前列表数据；带 BOM，Excel 直接打开中文不乱码）
 */

const csvCell = (v: any): string => {
    if (v === null || v === undefined) return ''
    let s = String(v)
    if (/[",\n\r]/.test(s)) s = '"' + s.replace(/"/g, '""') + '"'
    return s
}

const getVal = (obj: any, prop: string): any =>
    prop.split('.').reduce((o, k) => (o === null || o === undefined ? o : o[k]), obj)

export const exportCsv = (
    filename: string,
    columns: { label: string; prop: string }[],
    rows: any[]
): void => {
    if (!rows || rows.length === 0) {
        window.alert('当前无可导出的数据')
        return
    }
    const header = columns.map(c => csvCell(c.label)).join(',')
    const body = rows.map(r => columns.map(c => csvCell(getVal(r, c.prop))).join(','))
    const csv = '\uFEFF' + [header, ...body].join('\r\n')
    const blob = new Blob([csv], {type: 'text/csv;charset=utf-8;'})
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename.endsWith('.csv') ? filename : filename + '.csv'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
}

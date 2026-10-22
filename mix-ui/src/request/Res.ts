export interface Res {
    code: number,
    msg: string,
    /** 成功时返回的数据（如验证 token），失败为 null */
    data?: any
}
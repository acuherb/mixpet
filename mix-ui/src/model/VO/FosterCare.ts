/** 寄养每日照护记录 */
export interface FosterLog {
    logId: number;
    fosterId: number;
    logDate: string;
    mentalState?: string | null;
    appetite?: string | null;
    defecation?: string | null;
    weight?: number | null;
    note?: string | null;
    recorder?: string | null;
    createTime?: string;
}

/** 寄养每日照片（文件名不直接暴露给顾客，走 /foster/photo/file/{photoId} 归属校验下载） */
export interface FosterPhoto {
    photoId: number;
    fosterId: number;
    logId?: number | null;
    logDate: string;
    fileName?: string;
    originName?: string | null;
    createTime?: string;
}

/** 新增照护记录入参：文字 + 已上传好的照片文件名 */
export interface FosterLogVo {
    fosterId: number;
    logDate: string;
    mentalState?: string;
    appetite?: string;
    defecation?: string;
    weight?: number | null;
    note?: string;
    photos?: string[];
}

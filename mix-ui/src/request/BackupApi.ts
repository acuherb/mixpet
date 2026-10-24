import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";

// 备份文件信息
export interface BackupFileVo {
    name: string;
    size: number;
    time: string;
    /** 还原前自动快照 */
    snapshot: boolean;
}

// 备份文件列表
export const reqBackupList = (): Promise<BackupFileVo[]> =>
    reqApi<string, BackupFileVo[]>("backup/list")
// 立即备份（全库）
export const reqBackupCreate = (): Promise<Res> =>
    reqApi.post<string, Res>("backup/create")
// 删除备份
export const reqBackupDel = (name: string): Promise<Res> =>
    reqApi.delete<string, Res>("backup/" + name)
// 受控还原（仅超管；需二次确认文件名）
export const reqBackupRestore = (name: string, confirmName: string): Promise<Res> =>
    reqApi.post<string, Res>("backup/restore", {name, confirmName})

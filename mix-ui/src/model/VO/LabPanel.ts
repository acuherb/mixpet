import {LabTest} from "@/model/entiy/LabTest";

/**
 * 套组联查：套组头 + 成员单项
 */
export interface LabPanelVo {
    panel: LabTest;
    members: LabTest[];
}

/**
 * 套组保存请求
 */
export interface LabPanelSaveVo {
    panel: LabTest;
    memberIds: number[];
}

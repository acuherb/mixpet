/**
 * 个人信息
 */
export interface Client {
    clientId: number;
    clientUsername: string;
    clientPassword?: string | null;
    clientName: string;
    clientGender: boolean;
    clientTel: string;
    clientTel2: string | null;
    clientEmail: string | null;
    clientPwdChanged: boolean;
    active: boolean;
    clientBirthday: string | null;
    clientBirthdayEstimated: boolean;
    clientAddress: string | null;
    clientIdCard: string | null;
    clientLevel: number;
    clientPoints: number;
    clientBalance: number;
    clientSource: string | null;
    clientBlacklist: boolean;
    clientInfo: string;
    clientPhoto: string;
    auditStatus?: number;
}

/**
 * 空数据
 */
export const exampleClient = (): Client => {
    return {
        clientId: 0,
        clientUsername: "",
        clientName: "",
        clientGender: false,
        clientTel: "",
        clientTel2: null,
        clientEmail: null,
        clientPwdChanged: false,
        active: true,
        clientBirthday: null,
        clientBirthdayEstimated: false,
        clientAddress: null,
        clientIdCard: null,
        clientLevel: 0,
        clientPoints: 0,
        clientBalance: 0,
        clientSource: null,
        clientBlacklist: false,
        clientInfo: "",
        clientPhoto: "",
    }
}

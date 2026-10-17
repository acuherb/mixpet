/**
 * 医院基础信息（单行配置）
 */
export interface HospitalSetting {
    name: string;
    address: string;
    phone: string;
    /** logo 文件名（common/download?name=） */
    logo: string | null;
    bizHours: string;
    intro: string;
}

export const emptyHospital = (): HospitalSetting => ({
    name: '', address: '', phone: '', logo: null, bizHours: '', intro: ''
})

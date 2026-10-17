/**
 * 医院基础信息（单行配置）
 */
export interface HospitalSetting {
    name: string;
    address: string;
    phone: string;
    /** logo 文件名（common/download?name=） */
    logo: string | null;
    /** 品牌主题色(HEX)，注入 CSS 变量 --brand 全局生效 */
    brandColor?: string | null;
    bizHours: string;
    intro: string;
}

export const emptyHospital = (): HospitalSetting => ({
    name: '', address: '', phone: '', logo: null, brandColor: '#1E6FA8', bizHours: '', intro: ''
})

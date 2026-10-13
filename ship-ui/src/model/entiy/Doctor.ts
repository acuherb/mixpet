export interface Doctor {
    doctorId: number;
    departmentId?: number | null;
    username: string;
    doctorName: string;
    doctorGender: boolean;
    doctorAge: string;
    doctorPhoto: string;
    doctorTel: string;
    doctorEmail: string | null;
    authLv: number;
    jobId?: number | null;
    jobName?: string | null;
    doctorInfo: string;
    doctorMotto?: string | null;
    doctorPassword?: string | null;
    departmentIds?: number[];
    createTime?: string | null;
    updateTime?: string | null;
}

/**
 * 空数据
 */
export const exampleDoctor = (): Doctor => {
    return {
        doctorId: 0,
        departmentId: null,
        username: "",
        doctorName: "",
        doctorGender: false,
        doctorAge: "",
        doctorPhoto: "",
        doctorTel: "",
        doctorEmail: null,
        authLv: 0,
        jobId: null,
        jobName: null,
        doctorInfo: "",
        doctorMotto: null
    }
}
import {getNowISO} from "@/utils/TimeUtil";

export interface Appoint {
    appointmentId: number;
    clientId: number | null;
    petId: number | null;
    appointmentDate: string | null;
    departmentId: number | null;
    doctorId: number | null;
    appointmentInfo: string;
    isSuc: boolean | null;
    /** 关联的初诊病历id（复诊挂号） */
    originRecordId?: number | null;
    createTime?: string;
    updateTime?: string | null;
    isDel: string | null;
}

export const exampleAppoint = ():Appoint => {
    return {
        appointmentId: 0,
        clientId: null,
        petId: null,
        appointmentDate: getNowISO(),
        departmentId: null,
        doctorId: null,
        appointmentInfo: "",
        isSuc: null,
        originRecordId: null,
        isDel: null
    }
}

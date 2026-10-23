import {RecordDto} from "@/model/DO/RecordDto";
import {Prescription} from "@/model/entiy/MedicalRecord";
import {RecordPackage} from "@/model/entiy/ServicePackage";

/** 化验单（主表 + 明细） */
export interface LabOrderFull {
    order: {
        orderId: number;
        recordId: number;
        petId: number;
        clientId: number;
        doctorId: number;
        fee: number;
        status: boolean;
        orderNote?: string | null;
        createTime?: string | null;
    };
    items: {
        itemId: number;
        itemName: string;
        itemGroup?: string | null;
        itemUnit?: string | null;
        refRange?: string | null;
        resultValue?: string | null;
        isAbnormal?: boolean | null;
        resultTime?: string | null;
    }[];
}

/** 病例全量档案（对应后端 RecordFullVo） */
export interface RecordFull {
    record: RecordDto;
    prescriptions: Prescription[];
    packages: RecordPackage[];
    labOrders: LabOrderFull[];
    imageOrders: {
        orderId: number;
        itemName: string;
        groupName?: string | null;
        examPart?: string | null;
        fee?: number | null;
        status: boolean;
        reportText?: string | null;
        note?: string | null;
        createTime?: string | null;
    }[];
    hospitals: {
        hospitalId: number;
        recordId: number;
        petId: number;
        roomNo?: string | null;
        dailyFee?: number | null;
        inTime?: string | null;
        outTime?: string | null;
        status?: boolean | null;
        note?: string | null;
    }[];
    surgeries: {
        surgeryId: number;
        surgeryName: string;
        surgeryType?: string | null;
        anesthesiaType?: string | null;
        scheduledTime?: string | null;
        surgeon?: string | null;
        assistant?: string | null;
        fee?: number | null;
        status?: number | null;
    }[];
    immunes: {
        immuneId: number;
        recordId?: number | null;
        drugId?: number | null;
        drugName?: string | null;
        vaccinateTime?: string | null;
        nextTime?: string | null;
        times?: number | null;
        note?: string | null;
        doctorName?: string | null;
    }[];
    followUps: {
        followId: number;
        followType?: string | null;
        content?: string | null;
        result?: string | null;
        planTime?: string | null;
        followTime?: string | null;
        status?: boolean | null;
        followBy?: string | null;
    }[];
    /** 复诊链（后续复诊记录） */
    revisits: RecordDto[];
}

/** 病例检索条件 */
export interface RecordQuery {
    recordId?: number | null;
    petName?: string;
    clientName?: string;
    doctorName?: string;
    diagnosis?: string;
    status?: number | null;
    startTime?: string;
    endTime?: string;
    numPage: number;
    pageSize: number;
}

/** 诊疗待办统计 */
export interface RecordTodo {
    /** 接诊中的病历数（未完结） */
    treating: number;
    /** 待出结果的化验单数 */
    labPending: number;
    /** 待出报告的影像单数 */
    imagePending: number;
    /** 待进行/待记录的手术数 */
    surgeryPending: number;
    /** 待随访数 */
    followPending: number;
    /** 待审核的自助注册顾客数 */
    clientPending: number;
}

/**
 * 单个医生
 */
import {Doctor} from "@/model/entiy/Doctor";

export interface DoctorDto extends Doctor {
    departmentName: string;
    departmentIds: number[];
    departmentNames: string[];
}

/**
 * 医生个人中心
 */
export const exampleDoctorOne = (): DoctorDto => {
    return {
        "doctorId": 0,
        "departmentId": null,
        "username": "",
        "doctorName": "",
        "doctorGender": true,
        "doctorAge": "",
        "doctorPhoto": "",
        "doctorTel": "",
        "doctorEmail": null,
        "authLv": 0,
        "doctorInfo": "",
        "doctorMotto": "",
        "doctorPassword": null,
        "createTime": null,
        "updateTime": null,
        "departmentName": "",
        "departmentIds": [],
        "departmentNames": []
    }
}
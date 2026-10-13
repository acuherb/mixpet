import {Job} from "@/model/entiy/Job";

/**
 * 职称（联查该职称下医生数）
 */
export interface JobDto extends Job {
    doctorNum: number;
}

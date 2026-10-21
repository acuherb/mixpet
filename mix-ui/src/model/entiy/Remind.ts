import {RecordDto} from "@/model/DO/RecordDto";
import {ImmuneRecord} from "@/model/entiy/ImmuneRecord";

/**
 * 随访/复诊提醒
 */
export interface Remind {
    revisits: RecordDto[];
    vaccines: ImmuneRecord[];
}

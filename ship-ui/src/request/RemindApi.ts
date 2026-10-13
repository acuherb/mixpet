import {Remind} from "@/model/entiy/Remind";
import reqApi from "@/request/reqAPI";

// 随访/复诊提醒（应复诊 + 应接种，近30天）
export const reqRemind = (): Promise<Remind> =>
    reqApi<string, Remind>("remind")

import {StatOverview} from "@/model/entiy/StatOverview";
import reqApi from "@/request/reqAPI";

// 经营看板
export const reqStatOverview = (): Promise<StatOverview> =>
    reqApi<string, StatOverview>("statistics/overview")
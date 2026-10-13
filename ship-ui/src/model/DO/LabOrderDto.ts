import {LabOrder} from "@/model/entiy/LabOrder";
import {LabOrderItem} from "@/model/entiy/LabOrderItem";

/**
 * 化验单联查DTO
 */
export interface LabOrderDto extends LabOrder {
    petName?: string;
    clientName?: string;
    doctorName?: string;
    items?: LabOrderItem[];
}
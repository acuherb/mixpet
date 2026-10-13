package com.ship.model.dto;

import com.ship.model.entity.AnesthesiaRecord;
import com.ship.model.entity.Surgery;
import lombok.Data;

/**
 * 手术单联查 DTO：宠物/客户/医生名 + 麻醉记录
 */
@Data
public class SurgeryDto extends Surgery {
    private String petName;
    private String clientName;
    private String doctorName;
    /** 麻醉记录（详情时带出） */
    private AnesthesiaRecord anesthesia;
}

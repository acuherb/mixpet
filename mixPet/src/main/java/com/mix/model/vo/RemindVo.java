package com.mix.model.vo;

import com.mix.model.dto.ImmuneRecordDto;
import com.mix.model.dto.RecordDto;
import lombok.Data;

import java.util.List;

/**
 * 随访/复诊提醒：应复诊病历 + 应接种疫苗（近30天，含逾期）
 */
@Data
public class RemindVo {
    /** 应复诊（复诊日期在30天内，含逾期） */
    private List<RecordDto> revisits;
    /** 应接种（下一针在30天内） */
    private List<ImmuneRecordDto> vaccines;
}

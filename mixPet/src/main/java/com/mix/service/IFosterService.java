package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.FosterDto;
import com.mix.model.dto.FosterPetDto;
import com.mix.model.entity.Foster;
import com.mix.model.entity.FosterLog;
import com.mix.model.entity.FosterPhoto;
import com.mix.model.vo.FosterBackVo;
import com.mix.model.vo.FosterLogVo;

import java.util.List;

/**
 * 寄养服务：登记 → 入托 → 每日照护记录/照片 → 接回结算 → 账单与通知。
 */
public interface IFosterService extends IService<Foster> {

    boolean deleteById(Integer fosterId);

    boolean deleteByIds(List<Integer> idGroup);

    IPage<FosterDto> selectByPage(String clientName, String petName, int numPage, int pageSize);

    /** 前台匿名展示墙（脱敏，只含待入托/寄养中） */
    IPage<FosterPetDto> fosterPet(int pageSize);

    Integer haveFoster(Integer petId);

    boolean updateFoster(Foster foster);

    /** 待入托 → 寄养中，写入实际入托日并通知主人 */
    Result start(Integer fosterId);

    /** 接回结算：天数×单价定价（可人工改价留痕）、押金冲抵、净额入账、账单回指、通知 */
    Result back(Integer fosterId, FosterBackVo vo);

    /** 顾客「我的寄养」，强制按 client_id 过滤 */
    List<FosterDto> selfList(Integer clientId);

    FosterDto selfOne(Integer fosterId, Integer clientId);

    /** 员工单份详情（打印结算单） */
    FosterDto staffOne(Integer fosterId);

    /** 每日照护记录/照片；clientId 非空时按寄养归属校验，越权返回空 */
    List<FosterLog> logsOf(Integer fosterId, Integer clientId);

    List<FosterPhoto> photosOf(Integer fosterId, Integer clientId);

    Result addLog(FosterLogVo vo, String recorder);

    /** 单张照片读取（下载前置归属校验） */
    FosterPhoto photoForRead(Integer photoId, Integer clientId);
}

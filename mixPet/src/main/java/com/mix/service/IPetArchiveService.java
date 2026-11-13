package com.mix.service;

import com.mix.model.vo.PetArchive;

/**
 * 宠物就诊档案（全量聚合）
 */
public interface IPetArchiveService {
    PetArchive get(Integer petId);
}

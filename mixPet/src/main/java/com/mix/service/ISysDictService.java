package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.SysDict;

import java.util.List;

/**
 * 通用字典 Service
 */
public interface ISysDictService extends IService<SysDict> {
    /** 某类型的字典值（按 sort、id 升序；含停用，前端下拉可自行过滤启用） */
    List<SysDict> listByType(String dictType);
}

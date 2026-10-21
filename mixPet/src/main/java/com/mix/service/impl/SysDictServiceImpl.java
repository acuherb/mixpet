package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.SysDictMapper;
import com.mix.model.entity.SysDict;
import com.mix.service.ISysDictService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通用字典 Service 实现
 */
@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements ISysDictService {
    @Override
    public List<SysDict> listByType(String dictType) {
        return this.lambdaQuery()
                .eq(SysDict::getDictType, dictType)
                .orderByAsc(SysDict::getSort)
                .orderByAsc(SysDict::getDictId)
                .list();
    }
}

package com.ship.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MP:自定义元数据对象处理器
 */
@Component
public class FieldFill implements MetaObjectHandler {

    /**
     * 插入操作自动填充
     * 注意：必须判断实体是否有该 setter——否则对没有 updateTime 字段的实体会抛
     * ReflectionException: There is no setter for property named 'updateTime'
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        if (metaObject.hasSetter("createTime")) metaObject.setValue("createTime", now);
        if (metaObject.hasSetter("updateTime")) metaObject.setValue("updateTime", now);
    }

    /**
     * 更新操作自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime")) {
            metaObject.setValue("updateTime", LocalDateTime.now());
        }
    }
}

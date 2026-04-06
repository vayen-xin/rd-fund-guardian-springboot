package com.vayen.rdcm.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 用于自动填充 created_at 和 updated_at 字段
 */
@Slf4j
@Component // 必须加上，让Spring管理
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        
        // 如果实体类中字段名为 createdAt，则填充它
        if (metaObject.hasSetter("createdAt")) {
            strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        }
        // 如果实体类中字段名为 updatedAt，则填充它
        if (metaObject.hasSetter("updatedAt")) {
            strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        
        // 只填充 updated_at 字段
        if (metaObject.hasSetter("updatedAt")) {
            strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }
        // 注意：这里没有对 createdAt 进行 updateFill，实现了更新时不修改创建时间的要求
    }
}
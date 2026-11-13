package com.booth.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * create_time / update_time 由数据库 DEFAULT CURRENT_TIMESTAMP(3) 与 ON UPDATE 维护，
 * 应用层不写入，保证条件更新、批量 SQL 等绕过实体的写操作也能刷新 update_time（Canal 同步 ES 依赖它做版本号）。
 */
@Getter
@Setter
public abstract class BaseEntity extends BaseCreateEntity {

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}

package io.github.xiaotang.novartc.job.entity.mysql;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.Instant;

/**
 * 数据库实体公共基类，字段约定与 novartc-server 保持一致。
 *
 * @author xiaotang
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;
}

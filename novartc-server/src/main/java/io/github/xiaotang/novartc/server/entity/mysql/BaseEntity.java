package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * Created on 2026/3/18 14:02
 *
 * @author tangj
 * @description
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

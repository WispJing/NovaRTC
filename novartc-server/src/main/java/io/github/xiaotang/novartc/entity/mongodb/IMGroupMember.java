package io.github.xiaotang.novartc.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

/**
 * Created on 2025/2/28 15:36
 *
 * @author tangj
 * @description
 */
@Data
@Document(collection = "group_member")
public class IMGroupMember {
    @MongoId
    private String id;
    @Field("group_id")
    private String groupId;
    @Field("user_id")
    private String userId;
    @Field("joined_at")
    private Instant joinedAt;
}

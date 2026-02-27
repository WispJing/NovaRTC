package io.github.xiaotang.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

/**
 * Created on 2025/2/28 15:36
 *
 * @author tangj
 * @description
 */
@Data
@Document(collection = "group_member")
public class GroupMember {
    @MongoId
    private String id;
    @Field("group_member_id")
    private String groupMemberId;
    @Field("chat_id")
    private String chatId;
    @Field("user_id")
    private String userId;
    @Field("joined_at")
    private Date joinedAt;
}

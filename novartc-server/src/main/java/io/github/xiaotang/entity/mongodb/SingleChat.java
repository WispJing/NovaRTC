package io.github.xiaotang.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

/**
 * @author Tang_J on 2025/3/2 2:03
 * @Description TODO
 **/
@Data
@Document(collection = "single_chat")
public class SingleChat {
    @MongoId
    private String id;
    @Field("chat_id")
    private String chatId;
    @Field("user1_id")
    private String user1Id;
    @Field("user2_id")
    private String user2Id;
    @Field("created_at")
    private String createdAt;
}

package io.github.xiaotang.novartc.server.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

/**
 * @author Tang_J on 2025/3/2 2:03
 * @Description TODO
 **/
@Data
@Document(collection = "single_chat")
public class IMSingleChat {
    @MongoId
    private String id;
    @Field("chat_id")
    private String chatId;
    @Field("user1_id")
    private String user1Id;
    @Field("user2_id")
    private String user2Id;
    @Field("created_at")
    private Instant createdAt;
}

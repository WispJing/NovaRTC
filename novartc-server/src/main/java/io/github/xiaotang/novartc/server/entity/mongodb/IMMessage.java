package io.github.xiaotang.novartc.server.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

/**
 * @author Tang_J on 2025/3/2 1:34
 * @Description TODO
 **/
@Data
@Document(collection = "message")
public class IMMessage {
    @MongoId
    private String id;
    @Field("message_id")
    private String messageId;
    @Field("chat_id")
    private String chatId;
    @Field("sender_id")
    private String senderId;
    @Field("message_text")
    private String messageText;
    @Field("created_at")
    private Instant createdAt;
    @Field("chat_type")
    private Integer chatType;
}

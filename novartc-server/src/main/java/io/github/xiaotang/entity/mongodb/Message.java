package io.github.xiaotang.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

/**
 * @author Tang_J on 2025/3/2 1:34
 * @Description TODO
 **/
@Data
@Document(collection = "message")
public class Message {
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
    private Date createdAt;
    @Field("chat_type")
    private Integer chatType;
}

package io.github.xiaotang.novartc.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

/**
 * Created on 2025/2/28 15:31
 *
 * @author tangj
 * @description
 */
@Data
@Document(collection = "group_chat")
public class IMGroupChat {
    @MongoId
    private String id;
    @Field("chat_id")
    private String chatId;
    @Field("group_name")
    private String groupName;
    @Field("created_at")
    private Date createdAt;
}

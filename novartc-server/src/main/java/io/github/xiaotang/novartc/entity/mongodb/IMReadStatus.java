package io.github.xiaotang.novartc.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

/**
 * @author Tang_J on 2025/3/2 2:11
 * @Description TODO
 **/
@Data
@Document(collection = "read_status")
public class IMReadStatus {
    @MongoId
    private String id;
    @Field("read_status_id")
    private String read_status_id;
    @Field("message_id")
    private String message_id;
    @Field("user_id")
    private String user_id;
    @Field("read_at")
    private Instant read_at;
}

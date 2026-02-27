package io.github.xiaotang.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

/**
 * Created on 2025/2/28 13:56
 *
 * @author tangj
 * @description
 */
@Data
@Document(collection = "user")
public class User {
    @MongoId
    private String id;
    @Field("user_id")
    private String userId;
    @Field("username")
    private String username;
    @Field("password")
    private String password;
    @Field("email")
    private String email;
    @Field("created_at")
    private Date createdAt;
}

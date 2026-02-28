package io.github.xiaotang.novartc.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@CompoundIndex(
        name = "idx_id_time",
        def = "{'userId':1,'createdAt':-1}"
)
public class IMUser {
    @MongoId
    private String id;
    @Field("user_num")
    private String userNum;
    @Field("username")
    private String username;
    @Field("password")
    private String password;
    @Field("email")
    private String email;
    @Field("avatar_url")
    private String avatarUrl;
    @Field("created_at")
    private Date createdAt;
}

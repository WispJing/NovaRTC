package io.github.xiaotang.novartc.server.entity.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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
@CompoundIndexes({
        @CompoundIndex(name = "idx_chat_seq", def = "{'chat_id': 1, 'chat_seq': -1}"),
        @CompoundIndex(name = "idx_message_id", def = "{'message_id': 1}", unique = true)
})
public class IMMessage {
    @MongoId
    private String id;
    @Field("message_id")
    private String messageId;
    @Field("chat_seq")
    private long chatSeq;
    @Field("chat_id")
    private String chatId;
    @Field("sender_id")
    private String senderId;
    @Field("sender_name")
    private String senderName;
    @Field("sender_avatar_url")
    private String senderAvatarUrl;
    @Field("target_user_id")
    private String targetUserId;
    @Field("group_id")
    private String groupId;
    @Field("is_group")
    private Boolean isGroup;
    @Field("message_type")
    private String messageType;
    @Field("message_text")
    private String messageText;
    @Field("content_json")
    private String contentJson;
    @Field("server_time")
    private Instant serverTime;
    @Field("client_time")
    private Instant clientTime;
    @Field("chat_type")
    private Integer chatType;
    @Field("recalled")
    private Boolean recalled;
}

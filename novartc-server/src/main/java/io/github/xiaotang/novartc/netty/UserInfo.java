package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.entity.mongodb.IMUser;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created on 2026/2/28 15:24
 *
 * @author tangj
 * @description
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserInfo extends IMUser {
    private boolean isAuth = false; // 是否认证
    private long time = 0;  // 登录时间
    private String addr;    // 地址
    private Channel channel;// 通道
}

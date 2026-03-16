package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.service.IRedisService;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Created on 2026/3/16 16:01
 * <p>
 * 每个 chatId 独立递增
 * 用于保证会话内消息顺序
 *
 * @author tangj
 * @description 聊天会话序列号生成器
 */
@RequiredArgsConstructor
@Service
public class ChatSeqService {
    private final IMConfigProperties imConfigProperties;
    private final IRedisService redisService;

    /**
     * 获取下一个聊天序号
     */
    public long nextSeq(String chatId) {
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("ChatId is blank!");
        }
        String key = imConfigProperties.getChatSeqKeyPreFix() + chatId;
        // 原子自增
        return redisService.incr(key, 1);
    }
}

package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.service.IRedisService;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 会话序列号服务，负责为每个会话生成递增消息序号。
 */
@RequiredArgsConstructor
@Service
public class ChatSeqService {
    private final IMConfigProperties imConfigProperties;
    private final IRedisService redisService;

    /**
     * 获取指定会话的下一个消息序号。
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

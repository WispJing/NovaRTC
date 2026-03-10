package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.common.constant.IMConstant;
import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Created on 2026/3/10 11:40
 *
 * @author tangj
 * @description
 */
@Service
@RequiredArgsConstructor
public class UserRouteService {
    private final IMConfigProperties imConfigProperties;
    private final StringRedisTemplate redisTemplate;
    // 1小时，可根据心跳续期
    private static final long EXPIRE_SECONDS = 3600;

    private String getUserInstancesKey(String userId) {
        return imConfigProperties.getUserInstancesKeyPreFix() + userId;
    }

    private String getInstanceKey(String userId, String instanceId) {
        return imConfigProperties.getInstanceKeyPreFix() + userId + ":" + instanceId;
    }

    /**
     * 用户上线，记录实例
     */
    public void registerUser(String userId, String instanceId) {
        String instancesKey = getUserInstancesKey(userId);
        String instanceKey = getInstanceKey(userId, instanceId);
        // 将实例ID添加到用户的Set中
        redisTemplate.opsForSet().add(instancesKey, instanceId);
        // 设置实例Key，并设置过期时间
        redisTemplate.opsForValue().set(instanceKey, "online", EXPIRE_SECONDS, TimeUnit.SECONDS);
        ChatAccessLog.log.info("User {} registered instance {}", userId, instanceId);
    }

    /**
     * 用户下线，移除实例
     */
    public void unregisterUser(String userId, String instanceId) {
        String instancesKey = getUserInstancesKey(userId);
        String instanceKey = getInstanceKey(userId, instanceId);
        // 从Set中移除实例ID
        redisTemplate.opsForSet().remove(instancesKey, instanceId);
        // 删除实例Key
        redisTemplate.delete(instanceKey);
        ChatAccessLog.log.info("User {} unregistered instance {}", userId, instanceId);
    }

    /**
     * 续期指定用户实例
     */
    public void renewUser(String userId, String instanceId) {
        String instanceKey = getInstanceKey(userId, instanceId);
        Boolean exists = redisTemplate.expire(instanceKey, EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (exists) {
            ChatAccessLog.log.debug("Renewed instance {} for user {}", instanceId, userId);
        } else {
            ChatAccessLog.log.warn("Attempt to renew non-existing instance {} for user {}", instanceId, userId);
        }
    }

    /**
     * 获取用户所有在线实例ID（惰性清理过期实例）
     */
    public Set<String> getUserInstances(String userId) {
        String instancesKey = getUserInstancesKey(userId);
        Set<String> instanceIds = redisTemplate.opsForSet().members(instancesKey);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Set.of();
        }

        Set<String> activeInstances = new HashSet<>();
        for (String instanceId : instanceIds) {
            String instanceKey = getInstanceKey(userId, instanceId);
            Boolean exists = redisTemplate.hasKey(instanceKey);
            if (exists) {
                activeInstances.add(instanceId);
            } else {
                // 惰性删除：实例Key已过期，从Set中移除
                redisTemplate.opsForSet().remove(instancesKey, instanceId);
                ChatAccessLog.log.info("Removed expired instance {} for user {}", instanceId, userId);
            }
        }
        return activeInstances;
    }
}

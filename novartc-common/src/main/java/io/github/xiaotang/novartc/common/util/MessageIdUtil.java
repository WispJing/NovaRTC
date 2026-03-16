package io.github.xiaotang.novartc.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 雪花算法分布式ID生成器（优化版）
 * <p>
 * 64位ID结构：0(1bit) - 41bit时间戳 - 5bit数据中心ID - 5bit机器ID - 12bit序列号
 * <p>
 * 特性：
 * - 全局唯一，趋势递增
 * - 支持自定义起始时间戳（epoch）
 * - 时钟回退处理：微小回退（≤10ms）等待，大幅回退抛出异常
 * - 提供ID解析方法，便于调试
 *
 * @author tangj
 */
public class MessageIdUtil {

    /**
     * 起始时间戳（2024-01-01），可配置
     */
    private final long epoch;

    /**
     * 机器ID (0-31)
     */
    private final long workerId;

    /**
     * 数据中心ID (0-31)
     */
    private final long datacenterId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳（毫秒）
     */
    private long lastTimestamp = -1L;

    /**
     * 时钟回退最大容忍毫秒数
     */
    private static final long MAX_BACKWARD_MILLIS = 10L;

    /**
     * 各部分占用的bit数
     */
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 位移量
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 构造函数，使用默认起始时间（2024-01-01）
     *
     * @param workerId     机器ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public MessageIdUtil(long workerId, long datacenterId) {
        // 2024-01-01 00:00:00 UTC
        this(workerId, datacenterId, 1704067200000L);
    }

    /**
     * 全参构造函数，允许自定义起始时间戳
     *
     * @param workerId     机器ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     * @param epoch        起始时间戳（毫秒），不能大于当前时间
     */
    public MessageIdUtil(long workerId, long datacenterId, long epoch) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId must be between 0 and %d", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenterId must be between 0 and %d", MAX_DATACENTER_ID));
        }
        if (epoch > System.currentTimeMillis()) {
            throw new IllegalArgumentException("epoch cannot be greater than current time");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.epoch = epoch;
        // 随机初始化序列号，避免刚启动时序列号都是0，减少冲突概率（非必须）
        this.sequence = ThreadLocalRandom.current().nextLong(SEQUENCE_MASK + 1);
    }

    /**
     * 生成下一个全局唯一ID
     *
     * @return 64位长整型ID
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 处理时钟回退
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= MAX_BACKWARD_MILLIS) {
                // 微小回退：等待到 lastTimestamp
                timestamp = waitUntil(lastTimestamp);
            } else {
                throw new IllegalStateException(String.format(
                        "Clock moved backwards by %d ms (max tolerated: %d ms)", offset, MAX_BACKWARD_MILLIS));
            }
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号递增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒序列号耗尽，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置为随机值，避免起始总是0
            sequence = ThreadLocalRandom.current().nextLong(SEQUENCE_MASK + 1);
        }

        lastTimestamp = timestamp;

        // 拼接ID
        return ((timestamp - epoch) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待直到指定的时间戳
     *
     * @param targetTimestamp 目标时间戳（毫秒）
     * @return 到达目标时间戳后的当前时间
     */
    private long waitUntil(long targetTimestamp) {
        long timestamp;
        do {
            timestamp = currentTimeMillis();
        } while (timestamp < targetTimestamp);
        return timestamp;
    }

    /**
     * 等待下一毫秒（用于序列号溢出时）
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp;
        do {
            timestamp = currentTimeMillis();
        } while (timestamp <= lastTimestamp);
        return timestamp;
    }

    /**
     * 获取当前系统时间戳（毫秒）
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 从ID中解析出生成时间戳（毫秒，相对于epoch）
     *
     * @param id 雪花ID
     * @return 相对于epoch的毫秒数
     */
    public long extractTimestamp(long id) {
        return (id >>> TIMESTAMP_SHIFT) + epoch;
    }

    /**
     * 从ID中解析出数据中心ID
     *
     * @param id 雪花ID
     * @return 数据中心ID
     */
    public long extractDatacenterId(long id) {
        return (id >>> DATACENTER_ID_SHIFT) & ~(-1L << DATACENTER_ID_BITS);
    }

    /**
     * 从ID中解析出机器ID
     *
     * @param id 雪花ID
     * @return 机器ID
     */
    public long extractWorkerId(long id) {
        return (id >>> WORKER_ID_SHIFT) & ~(-1L << WORKER_ID_BITS);
    }

    /**
     * 从ID中解析出序列号
     *
     * @param id 雪花ID
     * @return 序列号
     */
    public long extractSequence(long id) {
        return id & SEQUENCE_MASK;
    }

    /**
     * 格式化输出ID的组成部分
     *
     * @param id 雪花ID
     * @return 可读的字符串
     */
    public String formatId(long id) {
        return String.format("ID: %d (timestamp: %d, datacenter: %d, worker: %d, sequence: %d)",
                id, extractTimestamp(id), extractDatacenterId(id), extractWorkerId(id), extractSequence(id));
    }
}
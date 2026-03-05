package io.github.xiaotang.novartc.netty;

import io.netty.util.HashedWheelTimer;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2026/3/5 15:09
 *
 * @author tangj
 * @description
 */
public class HeartbeatTimer {
    public static final HashedWheelTimer TIMER =
            new HashedWheelTimer(
                    1,
                    TimeUnit.SECONDS,
                    512
            );
}

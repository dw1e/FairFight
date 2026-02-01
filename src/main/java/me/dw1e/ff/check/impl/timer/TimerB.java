package me.dw1e.ff.check.impl.timer;

import com.google.common.collect.Lists;
import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

import java.util.Deque;

@CheckInfo(category = Category.TIMER, type = "B", desc = "使用'平均速度'检查游戏时间加速", minVL = -3.0, maxVL = 20)
public final class TimerB extends Check {

    // 此检查用于修复'余额'滥用导致 Timer A 被绕过

    private final Deque<Long> samples = Lists.newLinkedList();
    private Long lastTimestamp;

    public TimerB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            long timestamp = packet.getTimestamp();

            if (lastTimestamp != null) {
                long delay = timestamp - lastTimestamp;

                boolean exempt = data.getTick() < 40
                        || data.getTickSinceTeleport() < 4
                        || data.getTickSinceSteerVehicle() < 2;

                if (delay >= 5L && !exempt) samples.add(delay); // delay<5一般代表玩家丢包卡顿, 恢复了将一堆攒着的包同时发

                if (samples.size() >= 20) {
                    double mean = MathUtil.mean(samples), speed = 50.0 / mean;

                    if (mean <= 49.0) flag(String.format("speed=%.3f", speed), speed * 1.5);
                    else decreaseVL(0.5);

                    samples.clear();
                }
            }

            lastTimestamp = timestamp;
        }
    }

}

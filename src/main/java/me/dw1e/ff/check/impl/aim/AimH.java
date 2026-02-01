package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.evicting.EvictingList;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockPlace;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

// 分析攻击时鼠标移动速度与角度的非自然变化 (基于 Hawk 的思路)
@CheckInfo(category = Category.AIM, type = "H", desc = "启发式瞄准检查")
public final class AimH extends Check {

    private final Buffer buffer = new Buffer(7);

    private final EvictingList<Vector> mouseMoves = new EvictingList<>(5);
    private final EvictingList<Long> clickTimes = new EvictingList<>(5);

    public AimH(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            processMove();

        } else if ((packet instanceof CPacketUseEntity && ((CPacketUseEntity) packet).isAttack())
                || (packet instanceof CPacketBlockPlace && ((CPacketBlockPlace) packet).isPlacedBlock())) {
            processClick();
        }
    }

    private void processMove() {
        Location to = data.getLocation(), from = data.getLastLocation();

        Vector mouseMove = new Vector(to.getYaw() - from.getYaw(), to.getPitch() - from.getPitch(), 0);
        mouseMoves.add(mouseMove);

        if (clickedXMovesBefore()) {
            double minSpeed = Double.MAX_VALUE;
            double maxSpeed = 0.0;
            double maxAngle = 0.0;

            for (int i = 1; i < mouseMoves.size(); i++) {
                Vector lastMouseMove = mouseMoves.get(i - 1);
                Vector currMouseMove = mouseMoves.get(i);

                double speed = currMouseMove.length();
                double lastSpeed = lastMouseMove.length();
                double angle = lastSpeed != 0.0 ? MathUtil.angle(lastMouseMove, currMouseMove) : 0.0;

                if (Double.isNaN(angle)) angle = 0.0;

                minSpeed = Math.min(speed, minSpeed);
                maxSpeed = Math.max(speed, maxSpeed);
                maxAngle = Math.max(angle, maxAngle);

                if (maxSpeed - minSpeed > 4.0 && minSpeed < 0.01 && maxAngle < 0.1 && lastSpeed > 1.0) { // 断断续续
                    if (buffer.add() > 5) {
                        flag(String.format(
                                "stutter, maxSpeed=%.3f, minSpeed=%.3f, maxAngle=%.3f, lastSpeed=%.3f",
                                maxSpeed, minSpeed, maxAngle, lastSpeed
                        ));
                    }

                } else if (speed > 20.0 && lastSpeed > 20.0 && angle > 2.86) { // 抽搐 / 之字形
                    if (buffer.add() > 5) {
                        flag(String.format(
                                "twitching, speed=%.3f, lastSpeed=%.3f, angle=%.3f",
                                speed, lastSpeed, angle
                        ));
                    }

                } else if (speed - lastSpeed < -30.0 && angle > 0.8) { // 跳跃式不连续
                    if (buffer.add() > 5) {
                        flag(String.format(
                                "jump, speed=%.3f, lastSpeed=%.3f, angle=%.3f",
                                speed, lastSpeed, angle
                        ));
                    }

                } else buffer.multiply(0.99);
            }
        }
    }

    private void processClick() {
        long currTick = data.getTick();
        if (!clickTimes.contains(currTick)) clickTimes.add(currTick);
    }

    private boolean clickedXMovesBefore() {
        long time = data.getTick() - 2;

        for (int i = 0; i < clickTimes.size(); i++) {
            if (time == clickTimes.get(i)) {
                clickTimes.remove(i);
                return true;
            }
        }

        return false;
    }
}

package me.dw1e.ff.check.impl.speed;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

// Verus Speed E
@CheckInfo(category = Category.SPEED, type = "C", desc = "检查疾跑方向", maxVL = 15)
public final class SpeedC extends Check {

    private Double lastAngle;
    private int streaks;

    public SpeedC(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            if (!data.isSprinting() || !data.isLastClientGround() || !data.isClientGround()
                    || data.isNearBoat() || data.isNearWall() || data.isInLiquid() || data.isOnSlime()
                    || data.getTickSinceVelocity() <= data.getMaxVelocityTicks()
                    || data.getTickSinceTeleport() < 3
            ) lastAngle = null;

            double direction = Math.toDegrees(-Math.atan2(data.getDeltaX(), data.getDeltaZ()));

            double angle = Math.min(
                    MathUtil.distBetweenAngles360(direction, data.getLocation().getYaw()),
                    MathUtil.distBetweenAngles360(direction, data.getLastLocation().getYaw())
            );

            if (lastAngle != null) {
                double change = MathUtil.distBetweenAngles360(lastAngle, angle);

                if (angle > 50.5 && change < 5.1) {
                    if (++streaks > 3) {
                        streaks = 0;
                        flag(String.format("angle=%.7f\nchange=%.7f", angle, change));
                    }
                } else streaks = 0;
            }

            lastAngle = angle;
        }
    }

}

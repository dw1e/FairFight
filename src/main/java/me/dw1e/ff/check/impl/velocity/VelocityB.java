package me.dw1e.ff.check.impl.velocity;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.VELOCITY, type = "B", desc = "检查水平方向击退的修改")
public final class VelocityB extends Check {

    private final Buffer buffer = new Buffer(8);

    private double predictedXZ;

    public VelocityB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            int tick = data.getTickSinceVelocity(), tickOfCheck = 4;

            if (tick > tickOfCheck || data.isFlying()) return;

            if (tick == 1) predictedXZ = data.getVelocityXZ();

            if (data.isNearWall() || data.isInVehicle() || data.isInWeb() || data.isPushedByPiston()
                    || data.isClimbing() || data.getTickSinceTeleport() < 4) predictedXZ = 0.0;

            if (predictedXZ > 0.0) {
                if (data.getTickSinceAttack() == 1) predictedXZ *= 0.6F;

                if (data.isJumped() && data.isSprinting()) predictedXZ -= 0.2F; // 跳跃重置会增加0.2的跑跳加速

                float attribute = data.getAttributeSpeed();

                double offset = predictedXZ - data.getDeltaXZ() - attribute;

                if (offset > 1E-7) {
                    if (buffer.add() > tickOfCheck) flag(String.format("tick=%s, offset=%.7f", tick, offset));

                } else buffer.reduce(0.1);

                predictedXZ *= data.getFriction();
                if (predictedXZ < 0.005) predictedXZ = 0.0;
            }
        }
    }

}

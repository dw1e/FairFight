package me.dw1e.ff.check.impl.velocity;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.VELOCITY, type = "A", desc = "检查垂直方向击退的修改")
public final class VelocityA extends Check {

    private final Buffer buffer = new Buffer(8);

    private double predictedY;

    public VelocityA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            int tick = data.getTickSinceVelocity(), tickOfCheck = 4;

            if (tick > tickOfCheck || data.isFlying()) return;

            if (tick == 1) predictedY = data.getVelocityY();

            if (data.isJumped()) predictedY = data.getAttributeJump(); // 跳跃重置

            if (data.isUnderBlock() || data.isInVehicle() || data.isInWeb() || data.isClimbing()
                    || data.isPushedByPiston()
                    || data.getTickSinceTeleport() == 1
                    || data.getTickSinceOtherVelocity() == 1
            ) predictedY = 0.0;

            if (predictedY > 0.0) {
                double offset = predictedY - data.getDeltaY();
                double limit = data.isOffsetYMotion() ? 0.03 : 1E-7;

                if (offset > limit) {
                    if (buffer.add() > tickOfCheck) flag(String.format("tick=%s, offset=%.7f", tick, offset));

                } else buffer.reduce(0.1);

                if (data.isInWater()) predictedY = (predictedY * 0.8F) - 0.02;
                else if (data.isInLava()) predictedY = (predictedY * 0.5F) - 0.02;
                else predictedY = (predictedY - 0.08) * 0.98F;

                if (predictedY < 0.005) predictedY = 0.0;
            }
        }
    }

}

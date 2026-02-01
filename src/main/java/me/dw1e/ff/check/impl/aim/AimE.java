package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.AIM, type = "E", desc = "检查视角移动过于稳定", maxVL = 20)
public final class AimE extends Check {

    private final Buffer buffer = new Buffer(10);

    public AimE(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isRotation()
                && (data.getTickSinceAttack() < 5 || data.isBridging())) {
            float deltaYaw = data.getDeltaYaw(), deltaPitch = data.getDeltaPitch();
            float accelYaw = data.getAccelYaw(), accelPitch = data.getAccelPitch();

            if (isConstant(accelYaw, deltaYaw) || isConstant(accelPitch, deltaPitch)) {
                if (buffer.add() > 8)
                    flag(String.format("deltaYaw=%.2f, deltaPitch=%.2f\naccelYaw=%.2f, accelPitch=%.2f",
                            deltaYaw, deltaPitch, accelYaw, accelPitch));

            } else buffer.reduce();
        }
    }

    private boolean isConstant(float accel, float delta) {
        return accel < 0.1F && Math.abs(delta) > 1.5F;
    }
}

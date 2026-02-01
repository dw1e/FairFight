package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.AIM, type = "I", desc = "检查水平方向快速瞄准且垂直方向几乎不动的瞄准行为", maxVL = 20)
public final class AimI extends Check {

    private float buffer = 0.0F;

    public AimI(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isRotation()
                && (data.getTickSinceAttack() < 5 || data.isBridging())) {
            float deltaYaw = data.getDeltaYaw(), deltaPitch = data.getDeltaPitch();
            float pitch = data.getLocation().getPitch();

            if (Math.abs(pitch) <= 75.0F) {
                if (deltaYaw > 0.0F && deltaPitch < 1.0E-4F) {
                    buffer += Math.min(deltaYaw, 5.0F);

                    if (deltaPitch == 0.0F && deltaYaw > 10.0F) buffer += 5.0F;

                    if (buffer > 50.0F) {
                        buffer = 30.0F;

                        flag(String.format("deltaYaw=%.3f, deltaPitch=%.5f, pitch=%.3f", deltaYaw, deltaPitch, pitch));
                    }

                } else buffer = Math.max(0, buffer - 4.0F);
            }
        }
    }

}

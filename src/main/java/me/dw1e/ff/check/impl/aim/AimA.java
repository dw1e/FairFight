package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.AIM, type = "A", desc = "检查异常GCD", maxVL = 15)
public final class AimA extends Check {

    private final Buffer buffer = new Buffer(10);

    public AimA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isRotation()
                && (data.getTickSinceAttack() < 5 || data.isBridging())) {
            float deltaYaw = data.getDeltaYaw(), deltaPitch = data.getDeltaPitch();

            if (deltaYaw < 1.0F || deltaPitch < 1.0F) return;

            double divisor = MathUtil.gcd(deltaPitch, data.getLastDeltaPitch());

            if (divisor < 0.0078125F) {
                if (buffer.add(0.3) > 8) flag(String.format("divisor=%.7f", divisor));

            } else buffer.reduce(0.1);
        }
    }

}

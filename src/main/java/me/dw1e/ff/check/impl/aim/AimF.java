package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.AIM, type = "F", desc = "检查视角移动中的除数", maxVL = 10)
public final class AimF extends Check {

    private final Buffer buffer = new Buffer(10);

    public AimF(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isRotation()
                && (data.getTickSinceAttack() < 5 || data.isBridging())) {
            float deltaYaw = data.getDeltaYaw(), lastDeltaYaw = data.getLastDeltaYaw();
            float deltaPitch = data.getDeltaPitch(), lastDeltaPitch = data.getLastDeltaPitch();

            double constantYaw = MathUtil.gcd(deltaYaw, lastDeltaYaw);
            double constantPitch = MathUtil.gcd(deltaPitch, lastDeltaPitch);

            double currentX = deltaYaw / constantYaw, currentY = deltaPitch / constantPitch;
            double previousX = lastDeltaYaw / constantYaw, previousY = lastDeltaPitch / constantPitch;

            if (deltaYaw > 0.0 && deltaPitch > 0.0 && deltaYaw < 20.0F && deltaPitch < 20.0F) {
                double moduloX = currentX % previousX, moduloY = currentY % previousY;

                double floorModuloX = Math.abs(Math.floor(moduloX) - moduloX);
                double floorModuloY = Math.abs(Math.floor(moduloY) - moduloY);

                boolean invalidX = moduloX > 90.0 && floorModuloX > 0.1;
                boolean invalidY = moduloY > 90.0 && floorModuloY > 0.1;

                if (invalidX && invalidY) {
                    if (buffer.add() > 6) flag(String.format("mx=%.2f, my=%.2f, fmx=%.2f, fmy=%.2f",
                            moduloX, moduloY, floorModuloX, floorModuloY));

                } else buffer.reduce();
            }
        }
    }

}

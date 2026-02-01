package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.evicting.EvictingList;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.AIM, type = "G", desc = "检查视角异常平滑", maxVL = 15)
public final class AimG extends Check {

    private final Buffer buffer = new Buffer(20);

    private final EvictingList<Float>
            accelYawSamples = new EvictingList<>(20),
            accelPitchSamples = new EvictingList<>(20);

    public AimG(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isRotation()) {
            accelYawSamples.add(data.getAccelYaw());
            accelPitchSamples.add(data.getAccelPitch());

            if (accelYawSamples.isFull() && accelPitchSamples.isFull()) {
                double accelYawAVG = MathUtil.mean(accelYawSamples);
                double accelPitchAVG = MathUtil.mean(accelPitchSamples);

                double accelYawDev = MathUtil.deviation(accelYawSamples);
                double accelPitchDev = MathUtil.deviation(accelPitchSamples);

                boolean avgInvalid = accelYawAVG < 1.0 || accelPitchAVG < 1.0;
                boolean devInvalid = accelYawDev < 5.0 && accelPitchDev > 5.0;

                if (avgInvalid && devInvalid && data.getDeltaYaw() >= 1.5F) {
                    if (buffer.add() > 8) flag(String.format("yawAVG=%.2f, pitchAVG=%.2f\nyawDev=%.2f, pitchDev=%.2f",
                            accelYawAVG, accelPitchAVG, accelYawDev, accelPitchDev));

                } else buffer.reduce(0.75);
            }
        }
    }

}

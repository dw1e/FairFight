package me.dw1e.ff.check.impl.aim;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.AIM, type = "D", desc = "检查视角异常大幅度移动", minVL = -2.0)
public final class AimD extends Check {

    // 会因客户端卡顿或插件刚热加载运行导致误判, 不过误判VL通常不足以封禁

    public AimD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            if (data.getTick() < 20 || data.getTickSinceTeleport() < 3) return;

            float lastLastDeltaYaw = data.getLastLastDeltaYaw(), lastLastDeltaPitch = data.getLastLastDeltaPitch();
            float lastDeltaYaw = data.getLastDeltaYaw(), lastDeltaPitch = data.getLastDeltaPitch();
            float deltaYaw = data.getDeltaYaw(), deltaPitch = data.getDeltaPitch();

            boolean pitchExempt = Math.abs(data.getLocation().getPitch()) == 90.0F
                    || Math.abs(data.getLastLastLocation().getPitch()) == 90.0F;

            if (largeMove(deltaYaw, lastDeltaYaw, lastLastDeltaYaw))
                flag("yaw=" + lastDeltaYaw);

            if (largeMove(deltaPitch, lastDeltaPitch, lastLastDeltaPitch) && !pitchExempt)
                flag("pitch=" + lastDeltaPitch);
        }
    }

    private boolean largeMove(float now, float last, float lastLast) {
        return now == 0.0F && last >= 20.0F && lastLast == 0.0F;
    }
}

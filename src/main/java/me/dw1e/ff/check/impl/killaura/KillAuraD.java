package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

// Verus KillAura B
@CheckInfo(category = Category.KILL_AURA, type = "D", desc = "检查攻击频率", minVL = -1.0, maxVL = 4)
public final class KillAuraD extends Check {

    private int invalidTicks, lastTicks, totalTicks, ticks;

    public KillAuraD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) ++ticks;
        else if (packet instanceof CPacketUseEntity && ((CPacketUseEntity) packet).isAttack()) {

            // 大概意思就是你在1tick内攻击了多次, 这个检查来自Verus

            if (ticks <= 8) {
                if (lastTicks == ticks) ++invalidTicks;

                if (totalTicks++ >= 25) {
                    if (invalidTicks > 22)
                        flag(invalidTicks + "/" + totalTicks, 1.0 + (invalidTicks - 22) / 6.0);
                    else decreaseVL(1.0);

                    invalidTicks = totalTicks = 0;
                }

                lastTicks = ticks;
            }

            ticks = 0;
        }
    }

}

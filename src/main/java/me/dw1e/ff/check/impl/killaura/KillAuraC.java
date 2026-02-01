package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

@CheckInfo(category = Category.KILL_AURA, type = "C", desc = "检查同时攻击多个目标", maxVL = 1)
public final class KillAuraC extends Check {

    private int lastTarget, attacks;

    public KillAuraC(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketUseEntity) {
            CPacketUseEntity wrapper = ((CPacketUseEntity) packet);

            if (!wrapper.isAttack()) return;

            int target = wrapper.getEntityId();

            // 1tick内攻击两个不同的实体, 我觉得不可能
            if (target != lastTarget && ++attacks > 1) flag("attacks=" + attacks);

            lastTarget = target;
        } else if (packet instanceof CPacketFlying) attacks = 0;
    }

}

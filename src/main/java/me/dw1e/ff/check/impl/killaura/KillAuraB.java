package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketArmAnimation;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

@CheckInfo(category = Category.KILL_AURA, type = "B", desc = "检查在攻击时不摇摆手臂", maxVL = 3)
public final class KillAuraB extends Check {

    private boolean swung;

    public KillAuraB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketUseEntity && ((CPacketUseEntity) packet).isAttack() && !swung) flag();
        else if (packet instanceof CPacketArmAnimation) swung = true;
        else if (packet instanceof CPacketFlying) swung = false;
    }

}

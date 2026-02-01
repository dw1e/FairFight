package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockPlace;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

@CheckInfo(category = Category.KILL_AURA, type = "I", desc = "检查无效的攻击数据包顺序", maxVL = 3)
public final class KillAuraI extends Check {

    private boolean interacted, attacked;

    public KillAuraI(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) attacked = interacted = false;
        else if (packet instanceof CPacketUseEntity) {
            CPacketUseEntity wrapper = (CPacketUseEntity) packet;

            if (wrapper.isAttack()) attacked = true;

            if (wrapper.isInteract() || wrapper.isInteractAt()) interacted = true;

        } else if (packet instanceof CPacketBlockPlace && attacked && !interacted) flag();
    }

}

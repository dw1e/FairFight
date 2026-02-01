package me.dw1e.ff.check.impl.killaura;

import com.comphenix.protocol.wrappers.EnumWrappers;
import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockDig;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockPlace;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

@CheckInfo(category = Category.KILL_AURA, type = "F", desc = "检查非法的攻击与交互顺序", maxVL = 10)
public final class KillAuraF extends Check {

    private boolean dug, placed;

    public KillAuraF(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            dug = placed = false;
        } else if (packet instanceof CPacketBlockDig) {
            CPacketBlockDig wrapper = (CPacketBlockDig) packet;

            if (wrapper.getPlayerDigType() != EnumWrappers.PlayerDigType.DROP_ALL_ITEMS
                    && wrapper.getPlayerDigType() != EnumWrappers.PlayerDigType.DROP_ITEM) dug = true;

        } else if (packet instanceof CPacketUseEntity && ((CPacketUseEntity) packet).isAttack()) {
            if (placed || !dug || data.isInVehicle()) return;

            flag();
        } else if (packet instanceof CPacketBlockPlace) {
            placed = true;
        }
    }

}

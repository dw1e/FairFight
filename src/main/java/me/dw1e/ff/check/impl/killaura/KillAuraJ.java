package me.dw1e.ff.check.impl.killaura;

import com.comphenix.protocol.wrappers.EnumWrappers;
import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockDig;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

@CheckInfo(category = Category.KILL_AURA, type = "J", desc = "检查在攻击的同时破坏方块", maxVL = 3)
public final class KillAuraJ extends Check {

    // 瞎写的检查, 因为我发现新版水影会在穿墙攻击的同时挖掘方块(当然也有可能是我不会调配置)

    private boolean dug, attacked;

    public KillAuraJ(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketUseEntity && ((CPacketUseEntity) packet).isAttack()) attacked = true;

        else if (packet instanceof CPacketBlockDig && ((CPacketBlockDig) packet).getPlayerDigType()
                == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) dug = true;

        else if (packet instanceof CPacketFlying) {
            if (dug && attacked) flag();

            dug = attacked = false;
        }
    }

}

package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;

@CheckInfo(category = Category.KILL_AURA, type = "H", desc = "检查在使用物品时攻击", maxVL = 15)
public final class KillAuraH extends Check {

    private final Buffer buffer = new Buffer(5);

    public KillAuraH(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketUseEntity && ((CPacketUseEntity) packet).isAttack()) {

            if (data.isUsingItem()) {
                data.randomChangeSlot();

                if (buffer.add() > 3) flag();

            } else buffer.reduce(0.25);
        }
    }

}

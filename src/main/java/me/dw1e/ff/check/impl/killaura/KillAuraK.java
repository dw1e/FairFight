package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import org.bukkit.util.Vector;

@CheckInfo(category = Category.KILL_AURA, type = "K", desc = "检查开着Aura时挂机", maxVL = 1)
public final class KillAuraK extends Check {

    // 用于检查一些猪鼻挂机不关KillAura

    private Vector lastNoRotationCursorPos;
    private int stage;

    public KillAuraK(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            CPacketFlying wrapper = (CPacketFlying) packet;

            Vector cursorPos = new Vector(data.getLocation().getYaw(), data.getLocation().getPitch(), 0);
            boolean rotation = wrapper.isRotation();

            // 记录第一次未移动的视角
            if (stage == 0 && !rotation) {
                stage = 1;
                lastNoRotationCursorPos = cursorPos;
            }
            // 视角开始移动, 且必须有一定的移动幅度
            else if (stage == 1 && rotation && data.getDeltaYaw() > 1.0F && data.getDeltaPitch() > 1.0F) {
                stage = 2;
            }
            // 停止移动后, 视角又回到了最初的位置
            else if (stage == 2 && !rotation) {
                double delta = lastNoRotationCursorPos.distance(cursorPos);

                // 防止TP误判
                if (delta == 0.0 && data.getTickSinceTeleport() > 3) flag();

                stage = 0;
            }

        }

    }
}

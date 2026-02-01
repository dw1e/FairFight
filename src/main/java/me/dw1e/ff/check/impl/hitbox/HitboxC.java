package me.dw1e.ff.check.impl.hitbox;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.util.ServerUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;
import org.bukkit.util.Vector;

@CheckInfo(category = Category.HITBOX, type = "C", desc = "检查交互碰撞箱", maxVL = 1)
public final class HitboxC extends Check {

    // 目前只发现老版本水影(1.8.9)修改hitbox后右键交互实体时可被检测

    public HitboxC(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketUseEntity) {
            CPacketUseEntity wrapper = (CPacketUseEntity) packet;

            if (!wrapper.isInteractAt() || ServerUtil.getPlayerByEntityId(wrapper.getEntityId()) == null) return;

            Vector hitVec = wrapper.getHitVec();

            float x = (float) Math.abs(hitVec.getX());
            float y = (float) Math.abs(hitVec.getY());
            float z = (float) Math.abs(hitVec.getZ());

            if (x > 0.4F || y > 1.9F || z > 0.4F) flag("x=" + x + "\ny=" + y + "\nz=" + z);
        }
    }

}
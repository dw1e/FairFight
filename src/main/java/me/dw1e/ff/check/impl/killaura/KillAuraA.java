package me.dw1e.ff.check.impl.killaura;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.util.ServerUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import me.dw1e.ff.packet.wrapper.client.CPacketUseEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

@CheckInfo(category = Category.KILL_AURA, type = "A", desc = "检查在攻击时不减移速")
public final class KillAuraA extends Check {

    private final Buffer buffer = new Buffer(5);

    private boolean attacked;

    public KillAuraA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketUseEntity) {
            CPacketUseEntity wrapper = (CPacketUseEntity) packet;

            if (!wrapper.isAttack()) return;

            Player target = ServerUtil.getPlayerByEntityId(wrapper.getEntityId());

            boolean applies = data.isSprinting()
                    || data.getPlayer().getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK) > 0;

            if (target != null && applies) attacked = true;

        } else if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition() && attacked) {
            attacked = false;

            boolean invalid = !data.getEmulationProcessor().isHitSlowdown()
                    && data.getEmulationProcessor().isSprint() && data.isClientGround();

            if (invalid) {
                if (buffer.add() > 3) flag();

            } else buffer.reduce(0.2);
        }
    }

}

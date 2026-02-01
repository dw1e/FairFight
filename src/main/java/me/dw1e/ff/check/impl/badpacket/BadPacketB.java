package me.dw1e.ff.check.impl.badpacket;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import org.bukkit.Location;

@CheckInfo(category = Category.BAD_PACKET, type = "B", desc = "更新相同的视角位置", maxVL = 10)
public final class BadPacketB extends Check {

    // 可以检查一些神秘绕过工作时的缺陷

    public BadPacketB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isRotation()) {
            if (data.getTick() < 20 || data.getTickSinceTeleport() < 4 || data.getTickSinceSteerVehicle() < 3) return;

            Location from = data.getLastLocation(), to = data.getLocation();

            if (to.getYaw() == from.getYaw() && to.getPitch() == from.getPitch()) flag();
        }
    }

}

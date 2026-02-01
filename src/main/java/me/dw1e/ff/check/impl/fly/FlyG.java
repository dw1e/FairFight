package me.dw1e.ff.check.impl.fly;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.collision.Cuboid;
import me.dw1e.ff.misc.util.BlockUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.FLY, type = "G", desc = "检查爬墙(穿墙类)", maxVL = 15)
public final class FlyG extends Check {

    // 用以检查水影新版中 Spider 的 Polar 29 03 2025 模式

    private double lastFlagY; // 防止在同一位置一直触发导致封禁

    public FlyG(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            Cuboid in = new Cuboid(data.getLocation()).expandXZ(0.3F).expandY(0.0, 1.8F);

            boolean insideBlock = !in.checkMaterials(material ->
                    !BlockUtil.FULL_BLOCK.contains(material)); // 仅检查1x1x1的完整方块

            boolean exempt = data.getTickSinceTeleport() < 3 || data.isFlying()
                    || data.isPushedByPiston() || data.isInLiquid() || data.isClimbing();

            double deltaY = data.getDeltaY(), locY = data.getLocation().getY();

            if (insideBlock && deltaY > 0.4 && locY != lastFlagY && !exempt) {
                flag(String.format("deltaY=%.7f", deltaY));
                lastFlagY = locY;
                data.setback(PlayerData.SetbackType.SAFE_GROUND);
            }
        }
    }

}

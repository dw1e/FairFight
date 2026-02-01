package me.dw1e.ff.check.impl.misc;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.collision.Cuboid;
import me.dw1e.ff.misc.util.BlockUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

@CheckInfo(category = Category.MISC, type = "Phase", desc = "检查穿墙", punish = false)
public final class PhaseA extends Check {

    private final static Set<Material> EXEMPT_BLOCKS = EnumSet.of(Material.SOUL_SAND);

    private boolean lastInsideBlock;
    private Vector lastPistonPos = new Vector();

    public PhaseA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            Cuboid in = new Cuboid(data.getLocation()).expandXZ(0.3F).expandY(0.0, 1.8F);

            List<String> blocks = new ArrayList<>();

            boolean insideBlock = false;

            for (Block block : in.getBlocks()) {
                Material material = block.getType();

                if (BlockUtil.FULL_BLOCK.contains(material) && !EXEMPT_BLOCKS.contains(material)) {
                    insideBlock = true;
                    blocks.add(material.name());
                }
            }

            Vector pos = data.getLocation().toVector();

            if (data.isPushedByPiston()) lastPistonPos = pos;
            boolean pistonPos = data.getTickSincePushedByPiston() <= 21 && pos.distance(lastPistonPos) <= 1.0;

            boolean exempt = pistonPos || data.getTickSinceSteerVehicle() < 3
                    || data.getPlayer().getGameMode().equals(GameMode.SPECTATOR);

            if (!lastInsideBlock && insideBlock && !exempt) {
                flag("blocks=" + Arrays.toString(blocks.toArray()));
                data.setback(PlayerData.SetbackType.LAST_LOCATION);
            }

            lastInsideBlock = insideBlock;
        }
    }

}

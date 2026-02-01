package me.dw1e.ff.check.impl.interact;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.util.BlockUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockDig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CheckInfo(category = Category.INTERACT, type = "B", desc = "检查破坏被完全阻挡的方块", punish = false)
public final class InteractB extends Check {

    private final static BlockPosition[] OFFSETS = new BlockPosition[]{ // 6个方向的偏移量
            new BlockPosition(0, 1, 0),  // 上 (y + 1)
            new BlockPosition(0, -1, 0), // 下 (y - 1)
            new BlockPosition(0, 0, -1), // 北 (z - 1)
            new BlockPosition(0, 0, 1),  // 南 (z + 1)
            new BlockPosition(-1, 0, 0), // 西 (x - 1)
            new BlockPosition(1, 0, 0)}; // 东 (x + 1)

    public InteractB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketBlockDig) {
            CPacketBlockDig wrapper = (CPacketBlockDig) packet;

            // 可能在高延迟误判, 所以我选择只在方块被破坏掉后检查, 而不是刚摸到一下时
            if (wrapper.getPlayerDigType() != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) return;

            BlockPosition targetPos = wrapper.getBlockPosition();
            if (targetPos == null) return;

            int counts = 0; // 欲交互方块周围的方块数

            World world = data.getPlayer().getWorld();

            Block targetBlock = BlockUtil.getBlockAsync(targetPos.toLocation(world));

            if (targetBlock == null) return;

            List<String> blocksAround = new ArrayList<>();

            for (BlockPosition offset : OFFSETS) {
                BlockPosition neighborPos = new BlockPosition(
                        targetPos.getX() + offset.getX(),
                        targetPos.getY() + offset.getY(),
                        targetPos.getZ() + offset.getZ()
                );

                Block neighborBlock = BlockUtil.getBlockAsync(new Location(world,
                        neighborPos.getX(), neighborPos.getY(), neighborPos.getZ()));

                if (neighborBlock == null) continue;

                // 需要将床在此也算为一整格方块, 由于床是1x2的大小, 床的边上永远有另一张床, 而床是一个不完整方块
                if (BlockUtil.FULL_BLOCK.contains(neighborBlock.getType())
                        || neighborBlock.getType().equals(Material.BED_BLOCK)) {

                    counts++;
                    blocksAround.add(neighborBlock.getType().name());
                }
            }

            if (counts == 6) { // 交互的方块6个面都有方块(被完全包裹住)
                flag("target=" + targetBlock.getType().name()
                        + ", around=" + Arrays.toString(blocksAround.toArray()));

                // 此检测建议只取消非法操作, 不自动封禁, 因为卡顿会误判
                packet.setCancel(true);
                BlockUtil.resyncBlockAt(data.getPlayer(), targetPos.getX(), targetPos.getY(), targetPos.getZ());
            }
        }
    }

}

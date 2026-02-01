package me.dw1e.ff.misc.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.dw1e.ff.FairFight;
import me.dw1e.ff.misc.collision.Cuboid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class BlockUtil {

    public static final int[][] DIRECTIONS = {{0, -1, 0}, // 中心下方
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}, // 东西南北
            {-1, -1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, -1, -1}}; // 对角线

    public static final Set<Material> PASSABLE = new HashSet<>(), FULL_BLOCK = new HashSet<>(), STEP_ABLE = new HashSet<>();

    static {
        PASSABLE.addAll(Arrays.asList(Material.AIR, Material.GRASS, Material.SAPLING, Material.POWERED_RAIL,
                Material.DETECTOR_RAIL, Material.LONG_GRASS, Material.DEAD_BUSH, Material.YELLOW_FLOWER,
                Material.RED_ROSE, Material.TORCH, Material.FIRE, Material.CROPS, Material.SIGN_POST,
                Material.RAILS, Material.WALL_SIGN, Material.LEVER, Material.STONE_PLATE, Material.WOOD_PLATE,
                Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.STONE_BUTTON, Material.PORTAL,
                Material.TRIPWIRE, Material.CARROT, Material.POTATO, Material.WOOD_BUTTON, Material.GOLD_PLATE,
                Material.IRON_PLATE, Material.ACTIVATOR_RAIL, Material.STANDING_BANNER, Material.WALL_BANNER));

        FULL_BLOCK.addAll(Arrays.asList(Material.STONE, Material.GRASS, Material.DIRT, Material.COBBLESTONE,
                Material.BEDROCK, Material.SAND, Material.GRAVEL, Material.GOLD_ORE, Material.IRON_ORE,
                Material.COAL_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.DIAMOND_ORE,
                Material.EMERALD_ORE, Material.NETHERRACK, Material.QUARTZ_ORE, Material.ENDER_STONE, Material.OBSIDIAN,
                Material.SOUL_SAND, Material.MYCEL, Material.WOOD, Material.LOG, Material.LEAVES,
                Material.WOOD_DOUBLE_STEP, Material.BRICK, Material.TNT, Material.BOOKSHELF, Material.MOSSY_COBBLESTONE,
                Material.FURNACE, Material.BURNING_FURNACE, Material.WORKBENCH, Material.JUKEBOX, Material.DISPENSER,
                Material.DROPPER, Material.BEACON, Material.NOTE_BLOCK, Material.CLAY,
                Material.HARD_CLAY, Material.STAINED_CLAY, Material.SANDSTONE, Material.RED_SANDSTONE,
                Material.DOUBLE_STONE_SLAB2, Material.STONE_SLAB2, Material.NETHER_BRICK, Material.NETHER_FENCE,
                Material.QUARTZ_BLOCK, Material.PRISMARINE, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS,
                Material.SEA_LANTERN, Material.GLASS, Material.STAINED_GLASS, Material.IRON_BLOCK, Material.GOLD_BLOCK,
                Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK,
                Material.COAL_BLOCK, Material.SNOW_BLOCK, Material.ICE, Material.PACKED_ICE, Material.COMMAND));

        STEP_ABLE.addAll(Arrays.asList(Material.BED_BLOCK, Material.CAKE_BLOCK, Material.SNOW, Material.FLOWER_POT,
                Material.SKULL));

        for (Material material : Material.values()) {
            if (material.name().contains("STAIRS") || material.name().contains("STEP")) STEP_ABLE.add(material);
        }
    }

    public static Block getBlockAsync(Location location) {
        int cx = location.getBlockX() >> 4, cz = location.getBlockZ() >> 4;

        World world = location.getWorld();

        boolean chunkLoaded = world.isChunkLoaded(cx, cz);
        boolean chunkInUse = world.isChunkInUse(cx, cz);

        if (!chunkLoaded || !chunkInUse) return null;
        else return location.getWorld().getBlockAt(location);
    }

    @SuppressWarnings("deprecation")
    public static byte getBlockData(Block block) {
        return block.getData();
    }

    public static float getSlipperiness(Material material) {
        switch (material) {
            case SLIME_BLOCK:
                return 0.8F;
            case ICE:
            case PACKED_ICE:
                return 0.98F;
            default:
                return 0.6F;
        }
    }

    public static boolean isAdjacent(Cuboid region1, Cuboid region2) {
        boolean adjacentOnX = (region1.getMaxX() == region2.getMinX() || region1.getMinX() == region2.getMaxX())
                && region1.getMinY() <= region2.getMaxY() && region1.getMaxY() >= region2.getMinY()
                && region1.getMinZ() <= region2.getMaxZ() && region1.getMaxZ() >= region2.getMinZ();

        boolean adjacentOnY = (region1.getMaxY() == region2.getMinY() || region1.getMinY() == region2.getMaxY())
                && region1.getMinX() <= region2.getMaxX() && region1.getMaxX() >= region2.getMinX()
                && region1.getMinZ() <= region2.getMaxZ() && region1.getMaxZ() >= region2.getMinZ();

        boolean adjacentOnZ = (region1.getMaxZ() == region2.getMinZ() || region1.getMinZ() == region2.getMaxZ())
                && region1.getMinX() <= region2.getMaxX() && region1.getMaxX() >= region2.getMinX()
                && region1.getMinY() <= region2.getMaxY() && region1.getMaxY() >= region2.getMinY();

        return adjacentOnX || adjacentOnY || adjacentOnZ;
    }

    public static Block getSafeBlock(World world, int x, int y, int z) {
        if (world == null || y < 0 || y > 255) return null;

        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;

        try {
            return world.getBlockAt(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static void resyncBlock(Player player, Block block) {
        if (player == null || block == null) return;

        FairFight.INSTANCE.sendToMainThread(() -> {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);

            packet.getBlockPositionModifier().write(0, new BlockPosition(block.getLocation().toVector()));
            packet.getBlockData().write(0, WrappedBlockData.createData(block.getType(), block.getData()));

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        });
    }

    public static void resyncBlockAt(Player player, int x, int y, int z) {
        Block block = getSafeBlock(player.getWorld(), x, y, z);

        if (block != null) resyncBlock(player, block);
    }

    public static void resyncBlocksAround(Player player, Location loc) {
        if (player == null || loc == null) return;

        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();

        World world = loc.getWorld();
        if (world == null) return;

        for (int[] dir : DIRECTIONS) resyncBlockAt(player, x + dir[0], y + dir[1], z + dir[2]);
    }

}

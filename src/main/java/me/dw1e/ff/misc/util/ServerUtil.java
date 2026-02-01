package me.dw1e.ff.misc.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ServerUtil {

    public static Player getPlayerByEntityId(int entityId) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getEntityId() == entityId)
                .findFirst().orElse(null);
    }

    public static boolean isChunkLoaded(Location loc) {
        return loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)
                && loc.getWorld().isChunkInUse(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }
}

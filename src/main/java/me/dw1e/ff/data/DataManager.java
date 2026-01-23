package me.dw1e.ff.data;

import me.dw1e.ff.FairFight;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class DataManager {

    private final Map<UUID, PlayerData> dataMap = new HashMap<>();

    public void enable() {
        if (!Bukkit.isPrimaryThread()) {
            FairFight.INSTANCE.sendToMainThread(this::enable);
            return;
        }

        Bukkit.getOnlinePlayers().forEach(this::create);
    }

    public void disable() {
        dataMap.values().forEach(PlayerData::destroy);
        dataMap.clear();
    }

    public void create(Player player) {
        dataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(player));
    }

    public void delete(UUID uuid) {
        PlayerData data = dataMap.remove(uuid);

        if (data != null) data.destroy();
    }

    public PlayerData getData(UUID uuid) {
        return dataMap.getOrDefault(uuid, null);
    }

    public Map<UUID, PlayerData> getDataMap() {
        return dataMap;
    }

    public void toStaff(Consumer<PlayerData> action) {
        dataMap.values().stream().filter(PlayerData::isAlerts).forEach(action);
    }
}

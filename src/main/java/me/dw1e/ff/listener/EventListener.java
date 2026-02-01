package me.dw1e.ff.listener;

import me.dw1e.ff.FairFight;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.gui.Gui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class EventListener implements Listener {

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        FairFight.INSTANCE.getDataManager().create(event.getPlayer());
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        FairFight.INSTANCE.getDataManager().delete(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getPlayer().getUniqueId());

        if (data != null) data.reset();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChangedWorld(PlayerChangedWorldEvent event) {
        PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getPlayer().getUniqueId());

        if (data != null) data.reset();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void vehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player) {
            PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getEntered().getUniqueId());

            if (data != null) data.transConfirm(() -> data.setInVehicle(true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void vehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getExited().getUniqueId());

            if (data != null) data.transConfirm(() -> data.setInVehicle(false));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void vehicleDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle().getPassenger() instanceof Player) {
            PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getVehicle().getPassenger().getUniqueId());

            if (data != null) data.transConfirm(() -> data.setInVehicle(false));
        }
    }

    @EventHandler // GUI中的点击事件
    private void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (top.getHolder() instanceof Gui) {
            event.setCancelled(true);

            Inventory clicked = event.getClickedInventory();

            if (clicked != null && clicked.equals(top)) ((Gui) clicked.getHolder()).onClick(event);
        }
    }

    // 这是为了防止一些简单的Command Blocker隐藏指令, 就像AAC 4的效果一样
    @EventHandler(priority = EventPriority.MONITOR)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        Set<String> ffCMDs = new HashSet<>(Arrays.asList("ff", "fairfight:ff", "fairfight", "fairfight:fairfight"));

        String msg = FairFight.PREFIX + ChatColor.GRAY + " 此服务器正在使用 " + ChatColor.WHITE + "Fair Fight"
                + ChatColor.GRAY + "(v" + FairFight.INSTANCE.getPlugin().getDescription().getVersion() + ") 反作弊插件";

        for (String cmd : ffCMDs) if (event.getMessage().equals("/" + cmd)) event.getPlayer().sendMessage(msg);
    }

}

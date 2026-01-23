package me.dw1e.ff;

import com.comphenix.protocol.ProtocolLibrary;
import me.dw1e.ff.check.CheckManager;
import me.dw1e.ff.command.FairFightCommand;
import me.dw1e.ff.config.CheckValue;
import me.dw1e.ff.config.ConfigManager;
import me.dw1e.ff.config.ConfigValue;
import me.dw1e.ff.data.DataManager;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.gui.GuiManager;
import me.dw1e.ff.listener.EventListener;
import me.dw1e.ff.misc.ServerTickTask;
import me.dw1e.ff.packet.PacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

public enum FairFight {

    INSTANCE;

    public static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.YELLOW + "FF" + ChatColor.DARK_GRAY + "]";
    public static double TEST; // 测试用数值
    private FairFightPlugin plugin;
    private ConfigManager configManager;
    private DataManager dataManager;
    private CheckManager checkManager;
    private GuiManager guiManager;
    private ServerTickTask serverTickTask;
    private BukkitTask resetVLTask;

    public void onEnable(FairFightPlugin plugin) {

        // 一个简单的防倒卖判断, 能反编译改这块的也做不出这种事
        if (!plugin.getDescription().getName().equals("FairFight")
                || !plugin.getDescription().getAuthors().contains("dw1e")) {
            consoleLog(ChatColor.RED + "检测到插件信息篡改, 现已停止运行!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        if (!Bukkit.getServer().getVersion().contains("1.8.8")) {
            consoleLog(ChatColor.RED + "不支持的服务器版本: "
                    + ChatColor.YELLOW + Bukkit.getServer().getVersion()
                    + ChatColor.RED + ", 本插件仅支持 1.8.8 服务器!");

            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        String pLibDesc = ProtocolLibrary.getPlugin().getDescription().getVersion();
        int pLibVer = Integer.parseInt(pLibDesc.split("\\.")[0]);

        if (pLibVer < 5) {
            consoleLog(ChatColor.RED + "不支持的 ProtocolLib 版本: "
                    + ChatColor.YELLOW + pLibVer
                    + ChatColor.RED + ", 请使用 5.0.0 或之后的版本!");

            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        this.plugin = plugin;

        configManager = new ConfigManager();
        configManager.enable();

        checkManager = new CheckManager();
        checkManager.enable();

        dataManager = new DataManager();
        dataManager.enable();

        guiManager = new GuiManager();
        guiManager.enable();

        serverTickTask = new ServerTickTask();
        serverTickTask.enable();

        PluginCommand pluginCommand = plugin.getCommand("fairfight");

        if (pluginCommand != null) {
            FairFightCommand ffCommand = new FairFightCommand();

            pluginCommand.setExecutor(ffCommand);
            pluginCommand.setTabCompleter(ffCommand);
        }

        Bukkit.getPluginManager().registerEvents(new EventListener(), plugin);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketHandler(plugin));

        resetVLTask = getNewResetVLTask();
    }

    public void onDisable(FairFightPlugin plugin) {
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);

        Bukkit.getScheduler().cancelTasks(plugin);

        HandlerList.unregisterAll(plugin);

        if (resetVLTask != null) {
            resetVLTask.cancel();
            resetVLTask = null;
        }

        if (serverTickTask != null) {
            serverTickTask.disable();
            serverTickTask = null;
        }

        if (guiManager != null) {
            guiManager.disable();
            guiManager = null;
        }

        if (dataManager != null) {
            dataManager.disable();
            dataManager = null;
        }

        if (checkManager != null) {
            checkManager.disable();
            checkManager = null;
        }

        if (configManager != null) {
            configManager.disable();
            configManager = null;
        }

        this.plugin = null;
    }

    public void reloadConfig() {
        configManager.loadConfigs();
        checkManager.getCheckValueMap().values().forEach(CheckValue::updateConfig);

        guiManager.disable();
        guiManager.enable();

        if (resetVLTask != null) {
            resetVLTask.cancel();
            resetVLTask = getNewResetVLTask();
        }
    }

    private BukkitTask getNewResetVLTask() {
        return Bukkit.getScheduler().runTaskTimer(FairFight.INSTANCE.getPlugin(), () ->
                        dataManager.getDataMap().values().forEach(PlayerData::resetVL),
                0L, ConfigValue.violation_reset_interval * 60L * 20L);
    }

    public void sendToMainThread(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public FairFightPlugin getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ServerTickTask getServerTickTask() {
        return serverTickTask;
    }

    public void consoleLog(String s) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + s);
    }
}

package me.dw1e.ff.config;

import me.dw1e.ff.FairFight;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class ConfigValue {

    // 警报的配置
    public static String alerts_format;
    public static List<String> alerts_hover_message;
    public static String alerts_click_command;
    public static boolean alerts_print_to_console;
    public static boolean alerts_log_to_file;
    public static boolean alerts_interval_enabled;
    public static int alerts_interval_cooldown;
    // 警报的配置

    // VL重置间隔
    public static long violation_reset_interval;
    // VL重置间隔

    // 处罚的配置
    public static boolean punish_enabled;
    public static boolean punish_broadcast_enabled;
    public static List<String> punish_broadcast_messages;
    public static List<String> punish_commands;
    // 处罚的配置

    // 超时检测的配置
    public static boolean timeout_check_enabled;

    public static boolean timeout_check_transaction_enabled;
    public static long timeout_check_transaction_max_delay;
    public static String timeout_check_transaction_kick_message;
    public static String timeout_check_transaction_alert_message;

    public static boolean timeout_check_flying_enabled;
    public static long timeout_check_flying_max_delay;
    public static String timeout_check_flying_kick_message;
    public static String timeout_check_flying_alert_message;
    // 超时检测的配置

    // 杂项功能配置
    public static boolean ignore_high_version;
    // 杂项功能配置

    public static void update() {
        FileConfiguration config = FairFight.INSTANCE.getConfigManager().getConfig();

        // 警报的配置
        alerts_format = config.getString("alerts.format");
        alerts_hover_message = config.getStringList("alerts.hover_message");
        alerts_click_command = config.getString("alerts.click_command");
        alerts_print_to_console = config.getBoolean("alerts.print_to_console");
        alerts_log_to_file = config.getBoolean("alerts.log_to_file");
        alerts_interval_enabled = config.getBoolean("alerts.interval.enabled");
        alerts_interval_cooldown = config.getInt("alerts.interval.cooldown");
        // 警报的配置

        // VL重置间隔
        violation_reset_interval = config.getLong("violation_reset_interval");
        // VL重置间隔

        // 处罚的配置
        punish_enabled = config.getBoolean("punish.enabled");
        punish_broadcast_enabled = config.getBoolean("punish.broadcast.enabled");
        punish_broadcast_messages = config.getStringList("punish.broadcast.messages");
        punish_commands = config.getStringList("punish.commands");
        // 处罚的配置

        // 超时检测的配置
        timeout_check_enabled = config.getBoolean("timeout_check.enabled");

        timeout_check_transaction_enabled = config.getBoolean("timeout_check.transaction.enabled");
        timeout_check_transaction_max_delay = config.getLong("timeout_check.transaction.max_delay");
        timeout_check_transaction_kick_message = config.getString("timeout_check.transaction.kick_message");
        timeout_check_transaction_alert_message = config.getString("timeout_check.transaction.alert_message");

        timeout_check_flying_enabled = config.getBoolean("timeout_check.flying.enabled");
        timeout_check_flying_max_delay = config.getLong("timeout_check.flying.max_delay");
        timeout_check_flying_kick_message = config.getString("timeout_check.flying.kick_message");
        timeout_check_flying_alert_message = config.getString("timeout_check.flying.alert_message");
        // 超时检测的配置

        // 杂项功能配置
        ignore_high_version = config.getBoolean("ignore_high_version");
        // 杂项功能配置
    }

}

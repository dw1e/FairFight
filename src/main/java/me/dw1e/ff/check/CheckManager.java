package me.dw1e.ff.check;

import me.dw1e.ff.FairFight;
import me.dw1e.ff.api.event.FairFightFlagEvent;
import me.dw1e.ff.check.impl.aim.*;
import me.dw1e.ff.check.impl.autoclicker.AutoClickerA;
import me.dw1e.ff.check.impl.autoclicker.AutoClickerB;
import me.dw1e.ff.check.impl.autoclicker.AutoClickerC;
import me.dw1e.ff.check.impl.autoclicker.AutoClickerD;
import me.dw1e.ff.check.impl.badpacket.*;
import me.dw1e.ff.check.impl.fly.*;
import me.dw1e.ff.check.impl.hitbox.HitboxA;
import me.dw1e.ff.check.impl.hitbox.HitboxB;
import me.dw1e.ff.check.impl.hitbox.HitboxC;
import me.dw1e.ff.check.impl.interact.InteractA;
import me.dw1e.ff.check.impl.interact.InteractB;
import me.dw1e.ff.check.impl.interact.InteractC;
import me.dw1e.ff.check.impl.inventory.InventoryA;
import me.dw1e.ff.check.impl.inventory.InventoryB;
import me.dw1e.ff.check.impl.inventory.InventoryC;
import me.dw1e.ff.check.impl.inventory.InventoryD;
import me.dw1e.ff.check.impl.killaura.*;
import me.dw1e.ff.check.impl.post.*;
import me.dw1e.ff.check.impl.scaffold.*;
import me.dw1e.ff.check.impl.speed.SpeedA;
import me.dw1e.ff.check.impl.speed.SpeedB;
import me.dw1e.ff.check.impl.speed.SpeedC;
import me.dw1e.ff.check.impl.speed.SpeedD;
import me.dw1e.ff.check.impl.timer.TimerA;
import me.dw1e.ff.check.impl.timer.TimerB;
import me.dw1e.ff.check.impl.velocity.VelocityA;
import me.dw1e.ff.check.impl.velocity.VelocityB;
import me.dw1e.ff.check.impl.velocity.VelocityC;
import me.dw1e.ff.check.impl.velocity.VelocityD;
import me.dw1e.ff.config.CheckValue;
import me.dw1e.ff.config.ConfigValue;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.util.StringUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public final class CheckManager {

    private final List<Class<? extends Check>> checkList = new ArrayList<>();
    private final Map<String, CheckValue> checkValueMap = new HashMap<>(); // 检测们的配置文件
    private final Set<UUID> interval_player = new HashSet<>(); // 警报间隔时间内的玩家

    private final DecimalFormat decimalFormat = new DecimalFormat("0.###");

    public void enable() {
        addChecksToList();

        loadChecks(null).forEach(check -> checkValueMap.put(check.toString(), new CheckValue(check)));
        FairFight.INSTANCE.getConfigManager().saveChecks();
    }

    public void disable() {
        checkList.clear();
        checkValueMap.clear();
    }

    private void addChecksToList() {
        checkList.add(AimA.class);
        checkList.add(AimB.class);
        checkList.add(AimC.class);
        checkList.add(AimD.class);
        checkList.add(AimE.class);
        checkList.add(AimF.class);
        checkList.add(AimG.class);

        checkList.add(AutoClickerA.class);
        checkList.add(AutoClickerB.class);
        checkList.add(AutoClickerC.class);
        checkList.add(AutoClickerD.class);

        checkList.add(BadPacketA.class);
        checkList.add(BadPacketB.class);
        checkList.add(BadPacketC.class);
        checkList.add(BadPacketD.class);
        checkList.add(BadPacketE.class);
        checkList.add(BadPacketF.class);
        checkList.add(BadPacketG.class);
        checkList.add(BadPacketH.class);
        checkList.add(BadPacketI.class);
        checkList.add(BadPacketJ.class);
        checkList.add(BadPacketK.class);
        checkList.add(BadPacketL.class);
        checkList.add(BadPacketM.class);
        checkList.add(BadPacketN.class);
        checkList.add(BadPacketO.class);
        checkList.add(BadPacketP.class);
        checkList.add(BadPacketQ.class);

        checkList.add(FlyA.class);
        checkList.add(FlyB.class);
        checkList.add(FlyC.class);
        checkList.add(FlyD.class);
        checkList.add(FlyE.class);
        checkList.add(FlyF.class);
        checkList.add(FlyG.class);

        checkList.add(HitboxA.class);
        checkList.add(HitboxB.class);
        checkList.add(HitboxC.class);

        checkList.add(InteractA.class);
        checkList.add(InteractB.class);
        checkList.add(InteractC.class);

        checkList.add(InventoryA.class);
        checkList.add(InventoryB.class);
        checkList.add(InventoryC.class);
        checkList.add(InventoryD.class);

        checkList.add(KillAuraA.class);
        checkList.add(KillAuraB.class);
        checkList.add(KillAuraC.class);
        checkList.add(KillAuraD.class);
        checkList.add(KillAuraE.class);
        checkList.add(KillAuraF.class);
        checkList.add(KillAuraG.class);
        checkList.add(KillAuraH.class);
        checkList.add(KillAuraI.class);
        checkList.add(KillAuraJ.class);

        checkList.add(PostA.class);
        checkList.add(PostB.class);
        checkList.add(PostC.class);
        checkList.add(PostD.class);
        checkList.add(PostE.class);
        checkList.add(PostF.class);
        checkList.add(PostG.class);
        checkList.add(PostH.class);

        checkList.add(ScaffoldA.class);
        checkList.add(ScaffoldB.class);
        checkList.add(ScaffoldC.class);
        checkList.add(ScaffoldD.class);
        checkList.add(ScaffoldE.class);
        checkList.add(ScaffoldF.class);
        checkList.add(ScaffoldG.class);
        checkList.add(ScaffoldH.class);

        checkList.add(SpeedA.class);
        checkList.add(SpeedB.class);
        checkList.add(SpeedC.class);
        checkList.add(SpeedD.class);

        checkList.add(TimerA.class);
        checkList.add(TimerB.class);

        checkList.add(VelocityA.class);
        checkList.add(VelocityB.class);
        checkList.add(VelocityC.class);
        checkList.add(VelocityD.class);
    }

    public CheckValue getCheckValue(Check check) {
        return checkValueMap.getOrDefault(check.toString(), null);
    }

    public Map<String, CheckValue> getCheckValueMap() {
        return checkValueMap;
    }

    public List<Check> loadChecks(PlayerData data) {
        List<Check> checks = new ArrayList<>();

        for (Class<? extends Check> clazz : checkList) {
            try {
                checks.add(clazz.getConstructor(PlayerData.class).newInstance(data));

            } catch (Exception e) {
                FairFight.INSTANCE.consoleLog(ChatColor.RED + "加载检查 " + clazz.getSimpleName() + " 时发生错误!");

                throw new RuntimeException(e.getMessage());
            }
        }

        return checks;
    }

    public void handleVerbose(PlayerData data, Check check, String info) {
        CheckValue checkValue = getCheckValue(check);

        String format = StringUtil.color(String.format(
                "&8[&eFF&8] &8[&3V&8] &f%s&8.&f%s &8[&f%s&8/&7&o%s&8] &7%s",
                check.getCategory().getName(),
                check.getType(),
                decimalFormat.format(check.getViolations()),
                checkValue.getPunishVL(),
                info.replace("\n", ", ")
        ));

        data.getPlayer().sendMessage(format);
    }

    public void handleAlert(PlayerData data, Check check, String info) {
        CheckValue checkValue = getCheckValue(check);

        String format = replaceAndColor(ConfigValue.alerts_format, data, check)
                .replace("%vl%", decimalFormat.format(check.getViolations()))
                .replace("%max_vl%", String.valueOf(checkValue.getPunishVL()));

        TextComponent alertMessage = new TextComponent(format);

        StringBuilder hoverBuilder = new StringBuilder();

        List<String> hoverList = ConfigValue.alerts_hover_message;

        int hoverLines = 1;

        for (String eachHover : hoverList) {
            eachHover = StringUtil.color(eachHover)
                    .replace("%desc%", check.getDescription())
                    .replace("%info%", info.isEmpty() ? "无" : info);

            if (hoverLines++ == hoverList.size()) hoverBuilder.append(eachHover);
            else hoverBuilder.append(eachHover).append("\n");
        }

        alertMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverBuilder.toString()).create()));

        alertMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" +
                replaceAndColor(ConfigValue.alerts_click_command, data, check)));

        Bukkit.getPluginManager().callEvent(new FairFightFlagEvent(
                data.getPlayer(),
                check.getCategory().getName(),
                check.getType(),
                info.replace("\n", ", "),
                check.getViolations(),
                checkValue.getPunishVL(),
                check.getDescription()
        ));

        UUID uuid = data.getPlayer().getUniqueId();

        if (!interval_player.contains(uuid)) {
            FairFight.INSTANCE.getDataManager().toStaff(staff -> staff.getPlayer().spigot().sendMessage(alertMessage));

            if (ConfigValue.alerts_print_to_console) Bukkit.getConsoleSender().sendMessage(format);

            if (ConfigValue.alerts_interval_enabled) {
                interval_player.add(uuid);

                Bukkit.getScheduler().runTaskLater(FairFight.INSTANCE.getPlugin(), () ->
                        interval_player.remove(uuid), ConfigValue.alerts_interval_cooldown);
            }
        }

        if (ConfigValue.alerts_log_to_file) logToFile(data.getPlayer().getName(), String.format("%s | vl=%.2f | %s",
                check, check.getViolations(), info.replace("\n", ", ")));

        if (checkValue.isPunishable() && check.getViolations() >= checkValue.getPunishVL()) handlePunish(data, check);
    }

    public void handlePunish(PlayerData data, Check check) {
        if (!ConfigValue.punish_enabled || data.isPunished()) return;

        data.setPunished(true);

        if (ConfigValue.punish_broadcast_enabled) ConfigValue.punish_broadcast_messages.forEach(msg ->
                Bukkit.broadcastMessage(replaceAndColor(msg, data, check)));

        FairFight.INSTANCE.sendToMainThread(() -> ConfigValue.punish_commands.forEach(cmd ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaceAndColor(cmd, data, check))));
    }

    private String replaceAndColor(String message, PlayerData data, Check check) {
        return StringUtil.color(message
                .replace("%prefix%", FairFight.PREFIX)
                .replace("%player%", data.getPlayer().getName())
                .replace("%check%", check.getCategory().getName())
                .replace("%type%", check.getType())
        );
    }

    private void logToFile(String name, String log) {
        Path dir = FairFight.INSTANCE.getPlugin().getDataFolder().toPath().resolve("logs");
        Path file = dir.resolve(name.toLowerCase() + ".log");

        String date = "[" + new SimpleDateFormat("MM/dd/yy HH:mm:ss").format(new Date()) + "] ";

        try {
            Files.createDirectories(dir);

            try (BufferedWriter writer = Files.newBufferedWriter(file,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(date + log);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("无法为玩家 " + name + " 写入日志: ", e);
        }
    }
}

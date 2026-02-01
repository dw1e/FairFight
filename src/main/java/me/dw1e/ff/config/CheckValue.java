package me.dw1e.ff.config;

import me.dw1e.ff.FairFight;
import me.dw1e.ff.check.Check;
import org.bukkit.configuration.file.FileConfiguration;

public final class CheckValue {

    private final String name;

    private boolean enabled, punishable;
    private int punishVL;

    public CheckValue(Check check) {
        name = check.toString();

        FileConfiguration config = FairFight.INSTANCE.getConfigManager().getChecks();

        config.options().copyDefaults(true);

        config.addDefault(name + ".enabled", true);
        config.addDefault(name + ".punishable", check.isPunish());
        config.addDefault(name + ".punish_vl", check.getMaxViolations());

        updateConfig();
    }

    public void updateConfig() {
        FileConfiguration config = FairFight.INSTANCE.getConfigManager().getChecks();

        enabled = config.getBoolean(name + ".enabled");
        punishable = config.getBoolean(name + ".punishable");
        punishVL = config.getInt(name + ".punish_vl");
    }

    public String getName() {
        return name;
    }

    public boolean isPunishable() {
        return punishable;
    }

    public void setPunishable(boolean punishable) {
        this.punishable = punishable;

        editConfig("punishable", punishable);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        editConfig("enabled", enabled);
    }

    public int getPunishVL() {
        return punishVL;
    }

    private void editConfig(String path, Object value) {
        FairFight.INSTANCE.getConfigManager().getChecks().set(name + "." + path, value);
        FairFight.INSTANCE.getConfigManager().saveChecks();
    }
}

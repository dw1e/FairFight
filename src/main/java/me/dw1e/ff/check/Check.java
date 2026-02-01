package me.dw1e.ff.check;

import me.dw1e.ff.FairFight;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;

public abstract class Check {

    protected final PlayerData data;

    private final Category category;
    private final String type, description;
    private final double minViolations;
    private final int maxViolations;
    private final boolean punish;

    protected double violations;

    public Check(PlayerData data) {
        this.data = data;

        if (getClass().isAnnotationPresent(CheckInfo.class)) {
            CheckInfo info = getClass().getDeclaredAnnotation(CheckInfo.class);

            category = info.category();
            type = info.type();
            description = info.desc();
            minViolations = info.minVL();
            maxViolations = info.maxVL();
            punish = info.punish();
        } else throw new IllegalStateException("未在 " + getClass().getName() + " 中添加 CheckInfo 注释!");

        violations = minViolations;
    }

    public abstract void handle(WrappedPacket packet);

    protected void flag(String info, double addVL) {
        if (FairFight.INSTANCE.getServerTickTask().isLagging()) return;

        violations += addVL;

        if (data.isVerbose()) FairFight.INSTANCE.getCheckManager().handleVerbose(data, this, info);

        if (violations > 0.0) FairFight.INSTANCE.getCheckManager().handleAlert(data, this, info);
    }

    protected void flag(String info) {
        flag(info, 1.0);
    }

    protected void flag() {
        flag("", 1.0);
    }

    protected void decreaseVL(double amount) {
        violations -= Math.min(violations - minViolations, amount);
    }

    public Category getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getViolations() {
        return violations;
    }

    public void resetViolations() {
        violations = minViolations;
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    public boolean isPunish() {
        return punish;
    }

    @Override
    public String toString() {
        return category.getName().toLowerCase() + "." + type.toLowerCase();
    }
}

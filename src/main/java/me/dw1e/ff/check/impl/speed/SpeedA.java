package me.dw1e.ff.check.impl.speed;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CheckInfo(category = Category.SPEED, type = "A", desc = "检查水平方向速度修改", minVL = -3.0)
public final class SpeedA extends Check {

    private Double lastSpeed;
    private boolean lastSprinted, wasSneakOnEdge;
    private int lastFlagTicks;

    public SpeedA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            if (data.getTick() < 20 || data.isFlying()
                    || data.getTickSincePushedByPiston() < 2
                    || data.getTickSinceAbilityChange() < 3
                    || data.getTickSinceSteerVehicle() < 3
            ) lastSpeed = null;

            float friction = data.getFriction(), originalAttribute = data.getAttributeSpeed();

            boolean sprinting = data.isSprinting(), sneaking = data.isSneaking();

            if (lastSpeed != null) {
                List<String> tags = new ArrayList<>();

                double attribute = originalAttribute;

                if (data.isInLiquid()) tags.add(data.isInWater() ? "water" : "lava");

                boolean ground = data.isClientGround(), lastGround = data.isLastClientGround();

                if (lastGround) {
                    attribute *= 0.16277136F / Math.pow(friction, 3.0F);

                    tags.add("lastGround");

                    if (!ground && (data.getDeltaY() > 0.0 || data.isUnderBlock()) && sprinting) {
                        attribute += 0.2F;

                        tags.add("jumped");
                    }
                } else {
                    attribute = sprinting ? 0.026F : 0.02F;

                    tags.add("air");
                }

                if (sneaking) {
                    attribute *= 0.4158;

                    tags.add("sneaking");
                }

                if (data.isInWeb()) {
                    attribute *= 0.25;

                    tags.add("inWeb");
                }

                if (data.getTickSinceTeleport() == 1) {
                    lastSpeed = 0.0; // 传送时重置上次速度

                    tags.add("teleporting");
                }

                if (data.getTickSinceVelocity() == 1) {
                    attribute += data.getVelocityXZ();

                    tags.add("velocity");

                    if (data.getTickSinceTeleport() == 2) {
                        attribute += originalAttribute; // 末影珍珠

                        tags.add("enderPearl");
                    }
                }

                double excess = data.getDeltaXZ() - lastSpeed - attribute;

                double threshold = data.isOffsetMotion() ? 0.035 : 1E-6;

                // 一些屎山代码用以修复部分误判, 我的水平就到这了, 不要攻击我😭

                // 修复切换疾跑状态时的误判(攻击时的减速也算在内, 但玩家不松疾跑键不会发结束疾跑包)
                if (lastSprinted && !sprinting || data.getTickSinceAttack() < 9) threshold += 0.006F;

                // 修复在流体(有动画效果的液体)中的误判
                if (data.isInFlowingWater()) threshold += 0.02;
                if (data.getTickSinceInFlowingLava() < 2) threshold += originalAttribute * 0.825F;

                if (data.getTickSinceSteerVehicle() == 3) threshold = 0.04; // 离开载具概率误判
                if (data.getTickSinceTeleport() < 5) threshold = 0.05; // 修复一些传送时的误判

                if (data.isInWater() && friction != 0.8F) threshold = originalAttribute * 0.5F; // 修复深海探索者附魔误判

                if (wasSneakOnEdge) { // 潜行搭路, 下方块时的误判
                    threshold = originalAttribute; // 修复松开潜行下方块时的误判

                    tags.add("edge");
                }

                if (excess > threshold) {
                    lastFlagTicks = data.getTick();

                    flag(String.format("excess=%.7f/%.3f\ntags=%s", excess, threshold, Arrays.toString(tags.toArray())),
                            Math.max(0.5, (excess - threshold) * 5.0));

                    if (violations > 0.0) {
                        if (sneaking) data.resetSneak();

                        // 一个回弹用法的示例. 在有冗余阈值的检测中, 建议等阈值耗尽后再开始回弹(或其它惩罚)
                        data.setback(PlayerData.SetbackType.LAST_LOCATION);
                    }
                } else if (data.getTick() - lastFlagTicks > 20) decreaseVL(0.025);
            }

            lastSpeed = data.getDeltaXZ() * friction;
            lastSprinted = sprinting;
            wasSneakOnEdge = sneaking && data.isOnEdge(1.0);
        }
    }

}

package me.dw1e.ff.check.impl.fly;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CheckInfo(category = Category.FLY, type = "A", desc = "检查Y轴运动是否遵循游戏重力", minVL = -3.0)
public final class FlyA extends Check {

    // 直接改成滞空2ticks后再检测可以省去很多判断, 但也会出现很多绕过(比如瞬间下落, 或者一些我也不知道具体原理的Fly(Augustus的))

    private int streaks, lastLeaveLiquidTicks;

    public FlyA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && !((CPacketFlying) packet).isOnGround()) {
            double lastDeltaY = data.getLastDeltaY(), deltaY = data.getDeltaY();

            double predicted, threshold;

            float attribute = data.getAttributeJump();

            boolean lastGround = data.isLastClientGround(), offsetMotion = data.isOffsetYMotion();

            List<String> tags = new ArrayList<>();

            tags.add(deltaY > 0.0 ? "↑" : "↓");

            if (data.isInWater()) {
                predicted = (lastDeltaY * 0.8F) - 0.02; // 静态水的摩擦

                // 流动液体的具体判断要用nms, 我不想搞, 直接给个最大值吧
                // 玩家会在水平方向不动(或非常非常小的移动)时误判, 这个情况就触发 偏移运动(offsetMotion)
                threshold = data.isInFlowingWater() ? 0.155 : (offsetMotion ? 0.095 : 0.0400001F);

                tags.add("inWater");
            } else if (data.isInLava()) {
                predicted = (lastDeltaY * 0.5F) - 0.02; // 静态岩浆的摩擦

                // 玩家可以在最低一格的流动岩浆上跳跃
                threshold = data.isInFlowingLava() ? attribute * 1.6 : 0.125;

                tags.add("inLava");
            } else {
                if (lastGround) {
                    predicted = deltaY > 0.0 ? attribute : -0.0784F; // 起跳/刚开始下落

                    tags.add("lastGround");

                } else predicted = (lastDeltaY - 0.08) * 0.98F; // 空气的摩擦

                threshold = offsetMotion ? 0.05 : 1E-5; // 偏移运动
            }

            if (data.isInWeb()) {
                predicted *= 0.05; // 蜘蛛网内的摩擦

                tags.add("inWeb");
            }

            if (data.getTickSinceVelocity() == 1) {
                predicted = data.getVelocityY(); // 刚被击退时的移动就是击退包中的数值

                tags.add("velocity");
            }

            if (data.getTickSinceTeleport() < 3 && !data.isJumped()) {
                predicted = 0.0; // 修正传送时Y轴运动为0

                tags.add("teleporting");
            }

            // 老版本会抛弃掉过小的移动, 但是高版本不会, 所以这样写会让高版本误判, 但我只考虑1.7和1.8情况
            if (Math.abs(predicted) < 0.005) predicted = 0.0;

            if (data.getTickSinceSteerVehicle() < 5 // 刚出载具时可能误判
                    || data.getTickSinceOnSlime() < 38 // 从游戏最大下落速度-3.92砸到粘液块上时会被弹起最多37ticks的时间
                    || data.getJumpEffect() > 0 // 跳跃2会出现误判, 其它等级不会, 暂不清楚原因
            ) threshold = 0.0784001F; // 一些误判

            if (data.getLiquidTicks() == 1 || (data.isInLiquid() && !data.isInFlowingLiquid() && lastGround && deltaY > 0.0))
                threshold = Math.max(Math.max(attribute, Math.abs(lastDeltaY)), 0.5); // 入水第一刻的速度不可能比入水前还快

            double maxYMotion = Math.max(attribute, Math.abs(data.getVelocityY())); // 跳跃高度和击退高度, 哪个大取哪个
            double maxThreshold = Math.max(maxYMotion, 0.5); // 但是最小要给0.5

            if (data.getTickSinceOnSlime() < 9 // 从较高处下落被粘液块弹起时误判
                    || data.getTickSinceClimbing() < 2 // 刚离开梯子时
                    || data.getTickSinceVelocity() <= data.getMaxVelocityTicks() // 被击退后概率误判
                    || data.getTickSinceOtherVelocity() == 1 // 摔伤/烫伤等其它负击退
                    || (data.getTickSinceNearWall() < 3 && data.isInLiquid()) // 从液体中刚上岸
                    || (data.getTickSinceUnderBlock() < 2 && (data.getTickSinceClientGround() < 3
                    || Math.max(deltaY, lastDeltaY) > 0.0) && deltaY < maxYMotion) // 顶头跳, 地面刻<3为2格顶头
            ) {
                threshold = maxThreshold; // 设置阈值为跳跃/击退高度

            } else if (data.getTickSinceInLiquid() == 1) { // 在水面上下游动
                // 在水面按住空格每15ticks才会往下掉且加速一次, 此判断用于检查绕过
                if (data.getTick() - lastLeaveLiquidTicks >= 15) threshold = maxThreshold;

                tags.add("leaveLiquid");

                lastLeaveLiquidTicks = data.getTick();
            }

            // 登上台阶(床,台阶,楼梯等), 直接给个最大值, 省的每个都来个判断了
            if (data.getTickSinceNearStep() < 2 && data.getDeltaXZ() > 0.0) threshold = Math.max(0.6F, threshold);

            double offset = Math.abs(deltaY - predicted);

            boolean exempt = data.getTick() < 20 || data.isFlying() || data.isInUnloadedChunk()
                    || data.isOnSlime() || data.isClimbing()
                    || data.getTickSincePushedByPiston() < 4
                    || data.getTickSinceAbilityChange() < 3
                    || data.getTickSinceSteerVehicle() < 4;

            if (offset > threshold && !exempt) {
                flag(String.format("offset=%.7f/%.4f, streaks=%s\ntags=%s", offset, threshold, ++streaks,
                        Arrays.toString(tags.toArray())), Math.max(Math.min(streaks, 5), (offset - threshold) * 5.0));
            } else {
                streaks = 0;
                decreaseVL(0.025);
            }
        }
    }

}

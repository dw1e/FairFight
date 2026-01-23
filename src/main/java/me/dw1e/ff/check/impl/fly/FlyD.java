package me.dw1e.ff.check.impl.fly;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.FLY, type = "D", desc = "检查跳跃高度", minVL = -1.0)
public final class FlyD extends Check {

    private int streaks;

    public FlyD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            if (data.getTick() < 20 || data.isInUnloadedChunk()) return;

            double deltaY = data.getDeltaY(), attribute = data.getAttributeJump();

            boolean lastGround = data.isLastClientGround(), ground = data.isClientGround();

            // 检查Vanilla模式的Step, 或者一些其它作弊例如水影新版中Clip的Fancy模式
            check_step:
            {
                if (!(lastGround && ground) || data.getTickSinceTeleport() < 3) break check_step;

                if (deltaY > 0.6F || deltaY < -0.0784F) // 0.6是人物最高合法登上的高度, -0.0784是游戏重力下的最小下落速度
                    flag(String.format("step, deltaY=%.7f", deltaY), Math.max(1.0, Math.abs(deltaY) * 5.0));
            }

            // 检查直接从粘液块上起跳的跳跃高度, 或越跳越高, 高度无衰减
            check_slime:
            {
                if (!data.isOnSlime()) break check_slime;

                double absY = Math.abs(deltaY), lastLastAbsY = Math.abs(data.getLastLastDeltaY());

                double limit = attribute;

                if (data.getTickSinceVelocity() == 1) limit = Math.abs(data.getVelocityY());
                if (data.getTickSincePushedByPiston() < 3) limit = 1.0F;

                limit += 1E-7; // 精度问题

                // 检查从粘液块上的起跳高度是否 大于(增加) 或 等于(无衰减) 坠落速度d
                if (absY > limit && absY >= lastLastAbsY || lastLastAbsY != 0) flag(String.format("slime, %.7f ≥ %.7f", absY, lastLastAbsY),
                        Math.max(1.0, Math.round(absY - lastLastAbsY) * 5.0));
            }

            if (!(lastGround && !ground) || deltaY <= 0.0 || data.isFlying() || data.isOnSlime()
                    || data.getTickSincePushedByPiston() < 2
                    || data.getTickSinceVelocity() <= data.getMaxVelocityTicks()
            ) return;

            boolean hasFlag = false;

            // 检查跳的比正常高
            check_high_jump:
            {
                if (data.getTickSinceSteerVehicle() < 2) break check_high_jump;

                double limit = attribute;

                if (data.getTickSinceNearStep() < 2 && data.getDeltaXZ() > 0.0) limit = 0.6F;

                if (data.getJumpEffect() > 0 && data.getTickSinceNearStep() < 2)
                    limit += (data.getJumpEffect() * 0.1F) + 0.02F; // 修复在有跳跃药水效果时触发登台阶误判的问题

                double offset = deltaY - limit;

                if (offset > 1E-7) {
                    flag(String.format("high, offset=%.7f, streaks=%s", offset, ++streaks),
                            Math.max(Math.min(streaks, 5), offset * 5.0));
                    hasFlag = true;
                }
            }

            // 检查跳的比正常低
            check_low_jump:
            {
                if (data.isUnderBlock() || data.isClimbing() || data.isNearBoat() // 这里的船豁免是因为如果位置不同步玩家会卡住
                        || data.getTickSinceInWeb() < 3 // 在蜘蛛网内
                        || data.getTickSinceOnSlime() < 8 // 在粘液块上
                        || data.getTickSinceInLiquid() < 2 // 在水里
                        || data.getTickSinceOtherVelocity() == 1 // 烧伤时的负击退
                        || (data.isOffsetYMotion() && deltaY - 0.4044449F < 1E-7F) // 修复在偏移时向上搭/登高一格方块时的误判
                ) break check_low_jump;

                double offset = deltaY - attribute;

                if (offset < -1E-7) {
                    flag(String.format("low, offset=%.7f, streaks=%s", offset, ++streaks), Math.min(streaks, 5));
                    hasFlag = true;
                }
            }

            if (!hasFlag) {
                streaks = 0;
                decreaseVL(0.1); // 合法跳一下减0.1vl
            }

        }
    }

}

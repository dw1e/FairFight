package me.dw1e.ff.check.impl.fly;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.util.BlockUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.FLY, type = "F", desc = "检查利用幽灵方块欺骗地面状态", minVL = -3.0)
public final class FlyF extends Check {

    private int streaks;

    public FlyF(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            boolean client = data.isClientGround(), math = data.isMathGround();

            boolean exempt = data.getTick() < 20 || data.isFlying() || data.isInUnloadedChunk()
                    || data.getTickSinceNearStep() < 2;

            if (client && math && !exempt) {
                int airTicks = data.getTickSinceServerGround(), maxTicks = 1 + Math.min(data.getPingTicks(), 9);

                if (airTicks > maxTicks) {
                    if (++streaks > 1) { // 下落时搭方块接住会误判; 下落时靠近完整方块若触发了登台阶动作则误判; 目前使用一个缓冲解决误判
                        double vl = Math.min(streaks - 1, 5) * (data.isBridging() ? 0.3 : 1.0); // 减缓搭路时的误判

                        flag(String.format("airTicks=%s/%s, deltaY=%.7f, streaks=%s",
                                airTicks, maxTicks, data.getDeltaY(), streaks), vl);
                    }

                    // 防止幽灵方块误判, 触发检测时给玩家发送他脚下附近的方块更新包, 让其与服务器视角同步
                    BlockUtil.resyncBlocksAround(data.getPlayer(), data.getLocation());
                    if (violations > 0) data.setback(PlayerData.SetbackType.LAST_LOCATION);
                } else {
                    streaks = 0;
                    decreaseVL(0.025);
                }
            }
        }
    }

}

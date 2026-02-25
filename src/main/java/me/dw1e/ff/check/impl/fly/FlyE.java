package me.dw1e.ff.check.impl.fly;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.FLY, type = "E", desc = "检查攀爬速度", maxVL = 15)
public final class FlyE extends Check {

    public FlyE(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            if (!data.isClimbing() || data.isFlying() || data.getTickSincePushedByPiston() < 2
                    || data.getTickSinceVelocity() <= data.getMaxVelocityTicks()) return;

            float deltaY = (float) data.getDeltaY();

            if (deltaY != 0.0F) {
                if (deltaY > 0.0F) {
                    float limit = 0.1176F; // 向上爬的原版速度

                    int airTicks = data.getTickSinceClientGround();
                    if (airTicks <= 4 + data.getJumpEffect()) limit = data.getAttributeJump();

                    if (data.getTickSinceInLiquid() < 3) limit = 0.3F; // 在水中往上游时碰到梯子就是这么多
                    if (data.getTickSinceNearStep() < 2) limit = 0.6F; // 例如 爬悬空藤曼时走上台阶

                    if (deltaY > limit) flag(String.format("↑ deltaY=%s/%s, ticks=%s", deltaY, limit, airTicks),
                            Math.max(1.0, (deltaY - limit) * 5.0));

                } else {
                    float limit = -0.15F; // 向下爬的原版速度

                    // 比如 从高空下落时摸到梯子实现无伤落地的操作
                    if (data.getClimbingTicks() < 3) limit = (float) Math.min(data.getLastDeltaY() * 2.0F, -0.15F);

                    // 在水中爬时增加一些限制 (负数所以是减, 等价于limit += -0.0755F)
                    if (data.isInLiquid()) limit -= 0.0755F;

                    if (deltaY < limit) flag(String.format("↓ deltaY=%s/%s", deltaY, limit),
                            Math.max(1.0, (limit - deltaY) * 5.0));
                }
            }
        }
    }

}

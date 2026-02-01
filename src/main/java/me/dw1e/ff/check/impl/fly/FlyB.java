package me.dw1e.ff.check.impl.fly;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Buffer;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

@CheckInfo(category = Category.FLY, type = "B", desc = "检查客户端地面状态与Y轴对齐状态(1/64)", maxVL = 20)
public final class FlyB extends Check {

    private final Buffer buffer = new Buffer(5);

    public FlyB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketFlying && ((CPacketFlying) packet).isPosition()) {
            boolean clientGround = data.isClientGround(), mathGround = data.isMathGround();

            boolean exempt = data.getTick() < 20 || data.isFlying() || data.isOnSlime() || data.isUnderBlock()
                    || data.isInVehicle()
                    || (data.getTickSinceVelocity() == 1 && data.getVelocityY() % 0.015625 == 0.0)
                    || data.getTickSincePushedByPiston() < 5
                    || data.getTickSinceTeleport() < 3 // 传送时地面状态永远为否, 跳过
                    || data.getTickSinceNearStep() < 2;

            if (clientGround != mathGround && !exempt) {

                // 防止一些误判, 给一个快速衰减的缓冲, 但是不要衰减VL, 不然一些NoFall触发的VL可能不足以封禁
                if (buffer.add() > 1) flag(String.format("client=%s, math=%s", clientGround, mathGround));

            } else buffer.reduce(0.05);
        }
    }

}

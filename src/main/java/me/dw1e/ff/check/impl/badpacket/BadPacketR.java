package me.dw1e.ff.check.impl.badpacket;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;

/**
 * 检查移动包发送频率
 * @author StaR4y (Modified)
 * @date 2026-1-21
 */
@CheckInfo(category = Category.BAD_PACKET, type = "R", desc = "异常的数据包发送频率", maxVL = 15)
public class BadPacketR extends Check {

    public BadPacketR(PlayerData data) {
        super(data);
    }

    private long lastPacketTime = -1L;
    private long balance = 0L;

    //blink是这样写的
    @Override
    public void handle(WrappedPacket packet) {

        if (packet instanceof CPacketFlying) {
            long now = System.currentTimeMillis();

            if (lastPacketTime == -1L) {
                lastPacketTime = now;
                return;
            }
            long delay = now - lastPacketTime;
            balance -= delay;
            balance += 50;

            //防止误判
            if (balance < -50) balance = -50;
            if (balance > 250) {
                flag("Balance: " + balance + "ms, Delay: " + delay + "ms");
                balance = 0;
            }
            lastPacketTime = now;
        }
    }
}
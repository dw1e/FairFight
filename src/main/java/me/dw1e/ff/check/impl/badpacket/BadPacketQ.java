package me.dw1e.ff.check.impl.badpacket;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketTransaction;
import me.dw1e.ff.packet.wrapper.server.SPacketTransaction;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(category = Category.BAD_PACKET, type = "Q", desc = "异常的确认包回复情况", maxVL = 3, punish = false)
public final class BadPacketQ extends Check {

    // 如果服务器发送了一个 Transaction 包, 正常的合法客户端应该会按照顺序回复
    // 但这并非是协议里强制要求的, 所以作弊端可以: 不按顺序回复, 积攒着延迟回复, 伪造回复 和 预测回复 等
    // 这会干扰反作弊的延迟补偿机制, 所以添加此检测用于专门检查这种情况
    // 同时也有一个长时间不回复检查, 详见 PlayerData 中 864 行

    // 有可能会因为其它插件发送的 Transaction 包而误判, 这个检测只是打样, 你可以选择不开

    private final Deque<Short> pendingTransactions = new ArrayDeque<>(); // 等待确认的 Transaction

    public BadPacketQ(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketTransaction) {
            CPacketTransaction wrapper = (CPacketTransaction) packet;

            short action = wrapper.getActionId();

            if (action > 0) return; // actionId > 0 代表不是本反作弊发送的确认包的id范围

            // 在没有任何待确认的 Transaction 时却回复了一个 (预测)
            if (pendingTransactions.isEmpty()) {
                flag("unexpected");
                return;
            }

            short expected = pendingTransactions.peekFirst();

            // 顺序不匹配 (乱序, 跳号)
            if (expected != action) {
                flag("order, expected=" + expected + ", received=" + action);

                pendingTransactions.clear(); // 防止连锁误判
                return;
            }

            pendingTransactions.pollFirst(); // 正常确认

        } else if (packet instanceof SPacketTransaction) {
            SPacketTransaction wrapper = (SPacketTransaction) packet;

            short action = wrapper.getActionId();

            // windowId = 0 代表玩家背包(一般反作弊都用)
            if (wrapper.getWindowId() != 0 || action > 0) return;

            pendingTransactions.addLast(action);
        }
    }
}

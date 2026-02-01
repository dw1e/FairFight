package me.dw1e.ff.check.impl.scaffold;

import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.api.Category;
import me.dw1e.ff.check.api.annotations.CheckInfo;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketBlockPlace;
import org.bukkit.block.BlockFace;

@CheckInfo(category = Category.SCAFFOLD, type = "G", desc = "检查异常的放置成功率", maxVL = 10)
public final class ScaffoldG extends Check {

    private int success, failure;

    public ScaffoldG(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(WrappedPacket packet) {
        if (packet instanceof CPacketBlockPlace && data.isBridging() && !data.isSneaking()) {

            CPacketBlockPlace wrapper = (CPacketBlockPlace) packet;

            // 只检查搭路情况
            if (wrapper.getBlockFace() == BlockFace.DOWN || wrapper.getBlockFace() == BlockFace.UP) return;

            double deltaXZ = data.getDeltaXZ();
            if (data.getDeltaXZ() < data.getAttributeSpeed()) return;

            if (wrapper.isPlacedBlock()) ++success;
            else ++failure;

            // 玩家右键(不论是否真的放下了方块)即会发送BlockPlace包
            // 如果每次发送此包时都准确的放出了方块且搭路速度不慢, 那么他很可能在使用自动搭路

            // 打开 操作失误模拟 即可直接绕过, 用来检查一些开挂新手还行

            if (success + failure >= 10) {
                double ratio = (double) success / (success + failure);
                double limit = deltaXZ < 0.2 ? 0.9 : 0.6;

                if (ratio >= limit) flag(String.format("ratio=%s, deltaXZ=%.4f", ratio, deltaXZ));

                success = failure = 0;
            }

        }
    }

}

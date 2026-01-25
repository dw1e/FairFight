package me.dw1e.ff.packet;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.dw1e.ff.FairFight;
import me.dw1e.ff.FairFightPlugin;
import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.packet.wrapper.WrappedPacket;

public final class PacketHandler extends PacketAdapter {

    public PacketHandler(FairFightPlugin plugin) {
        super(plugin, ClassWrapper.getProcessedPackets());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.isCancelled()) return;

        PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getPlayer().getUniqueId());
        WrappedPacket wrappedPacket = ClassWrapper.wrapPacket(event.getPacketType(), event.getPacket());

        if (data != null) data.process(wrappedPacket);

        if (wrappedPacket.isCancel()) event.setCancelled(true);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isCancelled() || event.isPlayerTemporary()) return;

        PlayerData data = FairFight.INSTANCE.getDataManager().getData(event.getPlayer().getUniqueId());
        WrappedPacket wrappedPacket = ClassWrapper.wrapPacket(event.getPacketType(), event.getPacket());

        if (data != null) data.process(wrappedPacket);

        if (wrappedPacket.isCancel()) event.setCancelled(true);
    }
}

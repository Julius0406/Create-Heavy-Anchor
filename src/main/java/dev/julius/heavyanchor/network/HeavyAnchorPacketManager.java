package dev.julius.heavyanchor.network;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.network.packet.ClientboundTowRopeDataPacket;
import dev.julius.heavyanchor.network.packet.ClientboundTowRopeRemovePacket;
import foundry.veil.api.network.VeilPacketManager;

public final class HeavyAnchorPacketManager {
    public static final VeilPacketManager INSTANCE = VeilPacketManager.create(CreateHeavyAnchor.MODID, "0.1");

    private HeavyAnchorPacketManager() {
    }

    public static void init() {
        INSTANCE.registerClientbound(ClientboundTowRopeDataPacket.TYPE, ClientboundTowRopeDataPacket.CODEC, ClientboundTowRopeDataPacket::handle);
        INSTANCE.registerClientbound(ClientboundTowRopeRemovePacket.TYPE, ClientboundTowRopeRemovePacket.CODEC, ClientboundTowRopeRemovePacket::handle);
    }
}

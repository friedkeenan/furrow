package io.github.friedkeenan.furrow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FurrowClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FurrowPacket.TYPE, (packet, player, sender) -> {
            ((FurrowedEntity) player).setFurrow(packet.furrow);
        });
    }

}

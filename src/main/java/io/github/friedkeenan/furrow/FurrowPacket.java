package io.github.friedkeenan.furrow;

import java.util.Optional;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class FurrowPacket implements FabricPacket {
    public static final PacketType<FurrowPacket> TYPE = PacketType.create(new ResourceLocation("furrow:furrow"), FurrowPacket::new);

    public final Optional<Furrow> furrow;

    public FurrowPacket() {
        this.furrow = Optional.empty();
    }

    public FurrowPacket(Furrow furrow) {
        this.furrow = Optional.of(furrow);
    }

    public FurrowPacket(Optional<Furrow> furrow) {
        this.furrow = furrow;
    }

    public FurrowPacket(FriendlyByteBuf buf) {
        this.furrow = buf.readOptional(Furrow::readFromBuffer);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(this.furrow, (write_buf, furrow) -> furrow.writeToBuffer(write_buf));
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

}

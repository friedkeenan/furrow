package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NoEntityInteractionOutsideFurrow {
    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
        ),

        method      = "handleInteract",
        cancellable = true
    )
    private void cannotInteractOutsideFurrow(ServerboundInteractPacket packet, CallbackInfo info) {
        final var player          = this.getPlayer();
        final var furrowed_entity = (FurrowedEntity) player;

        furrowed_entity.getFurrow().ifPresent(
            f -> {
                if (!f.lenientIsWithinBounds(player.level, player)) {
                    info.cancel();
                }
            }
        );
    }
}

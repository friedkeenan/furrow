package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
@Environment(EnvType.CLIENT)
public class ClientsideCannotUseOnEntitiesOutsideFurrow {
    @Unique
    private Minecraft asMinecraft() {
        return (Minecraft) (Object) this;
    }

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/phys/EntityHitResult;getEntity()Lnet/minecraft/world/entity/Entity;"
        ),

        method      = "startUseItem",
        cancellable = true
    )
    private void cannotUseOnEntity(CallbackInfo info) {
        final var minecraft       = this.asMinecraft();
        final var furrowed_entity = (FurrowedEntity) minecraft.player;

        furrowed_entity.getFurrow().ifPresent(
            f -> {
                if (!f.lenientIsWithinBounds(minecraft.level, minecraft.player)) {
                    info.cancel();
                }
            }
        );
    }
}

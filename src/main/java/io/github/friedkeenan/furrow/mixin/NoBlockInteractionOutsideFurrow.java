package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

@Mixin(ServerLevel.class)
public class NoBlockInteractionOutsideFurrow {
    @Unique
    private ServerLevel asServerLevel() {
        return (ServerLevel) (Object) this;
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "mayInteract")
    private boolean cannotInteractOutsideFurrow(boolean original, @Local(argsOnly = true) Player player) {
        if (!original) {
            return false;
        }

        final var furrowed_entity = (FurrowedEntity) player;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        return furrowed_entity.getFurrow().get().lenientIsWithinBounds(this.asServerLevel(), player);
    }
}

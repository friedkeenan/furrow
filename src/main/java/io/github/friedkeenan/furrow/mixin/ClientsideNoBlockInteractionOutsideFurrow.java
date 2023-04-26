package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public class ClientsideNoBlockInteractionOutsideFurrow {
    @ModifyReturnValue(at = @At("RETURN"), method = "mayInteract")
    private boolean cannotInteractOutsideFurrow(boolean original, @Local(argsOnly = true) Player player) {
        if (!original) {
            return false;
        }

        final var furrowed_entity = (FurrowedEntity) player;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        return furrowed_entity.getFurrow().get().lenientIsWithinBounds((Level) (Object) this, player);
    }
}

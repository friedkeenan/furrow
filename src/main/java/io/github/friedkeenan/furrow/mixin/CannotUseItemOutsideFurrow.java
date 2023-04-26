package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public class CannotUseItemOutsideFurrow {
    @ModifyReturnValue(at = @At("RETURN"), method = "mayUseItemAt")
    private boolean cannotUseItemOutsideFurrow(boolean original) {
        if (!original) {
            return false;
        }

        final var furrowed_entity = (FurrowedEntity) this;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        final var player = (Player) (Object) this;

        return furrowed_entity.getFurrow().get().lenientIsWithinBounds(player.level, player);
    }
}

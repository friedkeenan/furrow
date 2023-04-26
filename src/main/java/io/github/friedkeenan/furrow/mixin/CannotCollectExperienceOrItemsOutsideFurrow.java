package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class CannotCollectExperienceOrItemsOutsideFurrow extends LivingEntity {
    private CannotCollectExperienceOrItemsOutsideFurrow(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("HEAD"), method = "touch", cancellable = true)
    private void cancelTouchOfExperienceAndItems(Entity touched, CallbackInfo info) {
        /*
            NOTE: Experience orbs still follow the player.
            We could change this, but I would like to change
            it for everything, i.e. mobs following the player.

            I also don't think it's that big of a deal.
        */

        if (!(touched instanceof ExperienceOrb || touched instanceof ItemEntity)) {
            return;
        }

        final var furrowed_entity = (FurrowedEntity) this;

        furrowed_entity.getFurrow().ifPresent(
            f -> {
                if (!f.lenientIsWithinBounds(this.level, this)) {
                    info.cancel();
                }
            }
        );
    }
}

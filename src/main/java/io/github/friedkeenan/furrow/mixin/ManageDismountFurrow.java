package io.github.friedkeenan.furrow.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.FurrowedEntity;
import io.github.friedkeenan.furrow.SafeDismount;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

@Mixin(DismountHelper.class)
public class ManageDismountFurrow {
    @Inject(
        at = @At("HEAD"),

        method = "canDismountTo(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;)Z",

        cancellable = true
    )
    private static void cannotDismountOutsideFurrow(
        CollisionGetter collision_getter, LivingEntity passenger, AABB hitbox,

        CallbackInfoReturnable<Boolean> info
    ) {
        final var furrowed_entity = (FurrowedEntity) passenger;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return;
        }

        final var is_within_furrow = furrowed_entity.getFurrow().get().isWithinBounds(passenger.level, hitbox);

        info.setReturnValue(is_within_furrow);
    }

    @ModifyExpressionValue(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/world/phys/AABB;)Z"
        ),

        method = "findSafeDismountLocation"
    )
    private static boolean unsafeOutsideFurrow(boolean inside_border, @Local(argsOnly = true) CollisionGetter collision_getter, @Local AABB hitbox) {
        if (!inside_border) {
            return false;
        }

        if (!(collision_getter instanceof Level)) {
            return true;
        }

        @Nullable final var entity = SafeDismount.DISMOUNTING_ENTITY.get();
        if (entity == null) {
            return true;
        }

        final var furrowed_entity = (FurrowedEntity) entity;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        /*
            NOTE: We use the supplied level and hitbox since
            the entity's current level and hitbox are not the
            same as the level and hitbox we should check.
        */
        return furrowed_entity.getFurrow().get().isWithinBounds((Level) collision_getter, hitbox);
    }
}

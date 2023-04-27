package io.github.friedkeenan.furrow.mixin;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(CollisionGetter.class)
public interface CheckNoFurrowCollision {
    @ModifyReturnValue(at = @At("RETURN"), method = "borderCollision")
    private @Nullable VoxelShape addFurrowCollisionToBorder(@Nullable VoxelShape border_shape, @Local(argsOnly = true) Entity entity) {
        final var furrow = ((FurrowedEntity) entity).getFurrow();
        if (furrow.isEmpty()) {
            return border_shape;
        }

        /*
            If the player is respawning, don't consider furrow collision.

            This is done to prevent the player from spawning at max build
            height when no available spawning space is found.
        */
        if (entity instanceof ServerPlayer && !furrow.get().isHorizontal()) {
            final var player = (ServerPlayer) entity;
            if (!player.server.getPlayerList().getPlayers().contains(player)) {
                return border_shape;
            }
        }

        if (border_shape == null) {
            return furrow.get().getCollisionShape(entity.level);
        }

        return Shapes.or(border_shape, furrow.get().getCollisionShape(entity.level));
    }

    @ModifyArg(
        at = @At(
            value  = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
        ),

        method = "findFreePosition"
    )
    private Predicate<VoxelShape> outsideFurrowIsNotFree(Predicate<VoxelShape> predicate, @Local(argsOnly = true) @Nullable Entity entity) {
        if (!(this instanceof Level)) {
            return predicate;
        }

        if (entity == null) {
            return predicate;
        }

        final var furrowed_entity = (FurrowedEntity) entity;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return predicate;
        }

        return predicate.and(shape -> furrowed_entity.getFurrow().get().isWithinBounds((Level) this, shape.bounds()));
    }
}

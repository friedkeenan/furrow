package io.github.friedkeenan.furrow.mixin;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.Furrow;
import io.github.friedkeenan.furrow.FurrowedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/* We have a higher priority for Lithium compatibility. */
@Mixin(value = Entity.class, priority = 1001)
public abstract class AddFurrow implements FurrowedEntity {
    private Optional<Furrow> furrow = Optional.empty();

    @Unique
    private Entity asEntity() {
        return (Entity) (Object) this;
    }

    @Override
    public Optional<Furrow> getFurrow() {
        return this.furrow;
    }

    @Override
    public void setFurrow(Optional<Furrow> furrow) {
        this.furrow = furrow;
    }

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"
        ),

        method = "load"
    )
    private void writeFurrow(CompoundTag data, CallbackInfo info) {
        this.furrow = Furrow.readFromCompound(data);
    }

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"
        ),

        method = "saveWithoutId"
    )
    private void readFurrow(CompoundTag data, CallbackInfoReturnable<CompoundTag> info) {
        this.furrow.ifPresent(f -> f.writeToCompound(data));
    }

    /*
        NOTE: We could use 'ModifyReturnValue' but then Lithium
        would bypass it with one of its optimizations. Because
        of that same optimization, we also inject at the head
        of the method instead of at the tail.
    */
    @Inject(at = @At("HEAD"), method = "isInWall", cancellable = true)
    private void suffocateOutsideFurrow(CallbackInfoReturnable<Boolean> info) {
        final var entity = this.asEntity();
        if (entity.noPhysics || this.furrow.isEmpty()) {
            return;
        }

        /* Passengers are given some grace due to collisions not happening. */
        if (entity.isPassenger()) {
            if(!this.furrow.get().overlapsWithBounds(entity.level, entity.getBoundingBox())) {
                info.setReturnValue(true);
            }

            return;
        }

        if (!this.furrow.get().isWithinBounds(entity.level, entity.getBoundingBox())) {
            info.setReturnValue(true);
        }
    }

    @Inject(at = @At("TAIL"), method = "rideTick")
    private void kickOffVehicleOutsideFurrow(CallbackInfo info) {
        /* The entity is a passenger at this point. */

        final var vehicle = this.asEntity().getVehicle();

        this.furrow.ifPresent(
            f -> {
                if (!f.overlapsWithBounds(vehicle.level, vehicle.getBoundingBox())) {
                    this.asEntity().stopRiding();
                }
            }
        );
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "canRide")
    private boolean cannotStartRidingOutsideFurrow(boolean original, @Local(argsOnly = true) Entity vehicle) {
        if (!original || this.furrow.isEmpty()) {
            return original;
        }

        /*
            NOTE: Theoretically a vehicle could position a
            passenger inside the passenger's furrow even if
            the vehicle itself is wholly outside the furrow.
            Checking this would be super ucky and wack for
            *extremely* little benefit, so we simply just
            check that at least some of the vehicle is within
            the potential passenger's furrow.
        */

        return this.furrow.get().overlapsWithBounds(vehicle.level, vehicle.getBoundingBox());
    }

    @ModifyVariable(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"
        ),

        method = "collideBoundingBox",

        require = 0
    )
    private static ImmutableList.Builder<VoxelShape> addFurrowCollision(
        ImmutableList.Builder<VoxelShape> builder,

        @Nullable Entity entity, Vec3 movement, AABB hitbox, Level level
    ) {
        if (entity == null) {
            return builder;
        }

        ((FurrowedEntity) entity).getFurrow().ifPresent(f -> builder.add(f.getCollisionShape(level)));

        return builder;
    }

    @ModifyVariable(
        at = @At("HEAD"),

        method = "lithiumCollideMultiAxisMovement",
        remap  = false,

        require = 0
    )
    private static List<VoxelShape> addFurrowCollisionForLithium(
        List<VoxelShape> other_collisions,

        @Nullable Entity entity, Vec3 movement, AABB hitbox, Level level
    ) {
        if (entity == null) {
            return other_collisions;
        }

        final var furrowed_entity = (FurrowedEntity) entity;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return other_collisions;
        }

        final var builder = ImmutableList.<VoxelShape>builderWithExpectedSize(other_collisions.size() + 1);

        builder.addAll(other_collisions);
        builder.add(furrowed_entity.getFurrow().get().getCollisionShape(level));

        return builder.build();
    }
}

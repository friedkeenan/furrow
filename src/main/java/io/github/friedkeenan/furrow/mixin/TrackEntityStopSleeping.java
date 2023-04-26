package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.friedkeenan.furrow.SafeDismount;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class TrackEntityStopSleeping extends Entity {
    private TrackEntityStopSleeping(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("HEAD"), method = "stopSleeping")
    private void trackEntityForSafeDismount(CallbackInfo info) {
        SafeDismount.DISMOUNTING_ENTITY.set(this);
    }
}

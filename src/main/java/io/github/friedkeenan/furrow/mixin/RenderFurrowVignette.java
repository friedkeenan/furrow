package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;

@Mixin(Gui.class)
@Environment(EnvType.CLIENT)
public class RenderFurrowVignette {
    @WrapOperation(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;getDistanceToBorder(Lnet/minecraft/world/entity/Entity;)D"
        ),

        method = "renderVignette"
    )
    private double showVignetteOutsideFurrow(WorldBorder border, Entity entity, Operation<Double> original) {
        final var furrowed_entity = (FurrowedEntity) entity;
        final var furrow          = furrowed_entity.getFurrow();

        if (furrow.isPresent() && !furrow.get().lenientIsWithinBounds(entity.level, entity)) {
            return 0.0;
        }

        return original.call(border, entity);
    }
}

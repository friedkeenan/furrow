package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.friedkeenan.furrow.FurrowedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
@Environment(EnvType.CLIENT)
public class ClientsideNoInteractionOutsideFurrow {
    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyExpressionValue(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/core/BlockPos;)Z"
        ),

        method = "startDestroyBlock"
    )
    private boolean cannotStartBreakBlock(boolean inside_border) {
        if (!inside_border) {
            return false;
        }

        final var furrowed_entity = (FurrowedEntity) this.minecraft.player;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        return furrowed_entity.getFurrow().get().lenientIsWithinBounds(this.minecraft.level, this.minecraft.player);
    }

    @ModifyExpressionValue(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/level/GameType;isCreative()Z"
        ),

        method = "continueDestroyBlock"
    )
    private boolean cannotContinueDestroyBlockInCreative(boolean is_creative) {
        if (!is_creative) {
            return false;
        }

        final var furrowed_entity = (FurrowedEntity) this.minecraft.player;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        return furrowed_entity.getFurrow().get().lenientIsWithinBounds(this.minecraft.level, this.minecraft.player);
    }

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;ensureHasSentCarriedItem()V",

            shift = At.Shift.AFTER
        ),

        method      = "useItemOn",
        cancellable = true
    )
    private void cannotUseItemOnBlock(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> info) {
        final var furrowed_entity = (FurrowedEntity) player;

        furrowed_entity.getFurrow().ifPresent(
            f -> {
                if (!f.lenientIsWithinBounds(player.level, player)) {
                    info.setReturnValue(InteractionResult.FAIL);
                }
            }
        );
    }
}

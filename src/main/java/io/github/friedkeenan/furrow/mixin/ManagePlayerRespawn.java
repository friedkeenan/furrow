package io.github.friedkeenan.furrow.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.SafeDismount;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerList.class)
public class ManagePlayerRespawn {
    @Inject(at = @At("HEAD"), method = "respawn")
    private void trackPlayerForSafeDismount(ServerPlayer player, boolean won_game, CallbackInfoReturnable<ServerPlayer> info) {
        if (player.getRespawnPosition() == null) {
            return;
        }

        SafeDismount.DISMOUNTING_ENTITY.set(player);
    }

    @ModifyVariable(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V",

            shift = At.Shift.AFTER
        ),

        ordinal = 1,

        method = "respawn"
    )
    private ServerPlayer spawnInsideFurrow(ServerPlayer new_player, @Local Optional<Vec3> respawn_pos) {
        if (respawn_pos.isEmpty()) {
            /* Call 'fudgeSpawnLocation' again after the player's furrow has been potentially set. */
            ((ServerPlayerAccessor) new_player).callFudgeSpawnLocation((ServerLevel) new_player.level);
        }

        return new_player;
    }
}

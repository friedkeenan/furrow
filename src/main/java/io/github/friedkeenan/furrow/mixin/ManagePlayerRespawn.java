package io.github.friedkeenan.furrow.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.FurrowedEntity;
import io.github.friedkeenan.furrow.SafeDismount;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
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
        if (respawn_pos.isPresent()) {
            return new_player;
        }

        /* The player was not able to respawn at a specified respawn location. */

        final var furrowed_entity = (FurrowedEntity) new_player;
        furrowed_entity.getFurrow().ifPresent(
            f -> {
                final var bound_info = f.getBoundInfo(new_player.level);

                final var tentative_pos = new_player.position()
                    .with(bound_info.axis(), bound_info.intercept());

                final var new_pos = PlayerRespawnLogicInvoker.callGetOverworldRespawnPos(
                    (ServerLevel) new_player.level,
                    Mth.floor(tentative_pos.x()),
                    Mth.floor(tentative_pos.z())
                );

                new_player.moveTo(new_pos, 0, 0);

                if (bound_info.axis().isVertical() && new_player.getY() < bound_info.min()) {
                    /*
                        NOTE: If the furrow is horizontal, then
                        we try to place the player either on the
                        highest block or on the bottom of the furrow
                        if the highest block is beneath it.

                        If the highest block is above the top of the
                        furrow, then the player will be spawned there,
                        outside their furrow.
                    */

                    new_player.moveTo(new_player.position().with(Direction.Axis.Y, bound_info.min()));
                }
            }
        );

        return new_player;
    }
}

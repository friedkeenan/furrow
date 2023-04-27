package io.github.friedkeenan.furrow.mixin;

import java.util.Optional;

import javax.swing.text.html.parser.Entity;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.friedkeenan.furrow.Furrow;
import io.github.friedkeenan.furrow.FurrowPacket;
import io.github.friedkeenan.furrow.FurrowedEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

@Mixin(ServerPlayer.class)
public class ManagePlayerFurrow {
    Optional<Furrow> last_sent_furrow = Optional.empty();

    @Unique
    private ServerPlayer asServerPlayer() {
        return (ServerPlayer) (Object) this;
    }

    @Inject(at = @At("HEAD"), method = "doTick")
    private void sendFurrow(CallbackInfo info) {
        final var furrowed_entity = (FurrowedEntity) this;

        final var furrow = furrowed_entity.getFurrow();

        if (!furrow.equals(this.last_sent_furrow)) {
            ServerPlayNetworking.send(this.asServerPlayer(), new FurrowPacket(furrow));

            this.last_sent_furrow = furrow;
        }
    }

    @Inject(at = @At("HEAD"), method = "restoreFrom")
    private void restoreFurrow(ServerPlayer other, boolean won_game, CallbackInfo info) {
        final var furrowed_self  = (FurrowedEntity) this;
        final var furrowed_other = (FurrowedEntity) other;

        furrowed_self.setFurrow(furrowed_other.getFurrow());

        this.last_sent_furrow = Optional.empty();
    }

    @Inject(
        at = @At(
            value  = "FIELD",
            target = "Lnet/minecraft/server/level/ServerPlayer;lastSentExp:I",
            opcode = Opcodes.PUTFIELD
        ),

        method = "changeDimension"
    )
    private void sendFurrowOnChangeDimension(ServerLevel level, CallbackInfoReturnable<@Nullable Entity> info) {
        this.last_sent_furrow = Optional.empty();
    }

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V",

            ordinal = 0,
            shift   = At.Shift.AFTER
        ),

        method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"
    )
    private void sendFurrowAfterTeleportChangeDimension(ServerLevel level, double x, double y, double z, float pitch, float yaw, CallbackInfo info) {
        ServerPlayNetworking.send(this.asServerPlayer(), new FurrowPacket(this.last_sent_furrow));
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "isReachableBedBlock")
    private boolean bedOutsideFurrowIsUnreachable(boolean original, @Local(argsOnly = true) BlockPos pos) {
        if (!original) {
            return original;
        }

        final var furrowed_entity = (FurrowedEntity) this;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return true;
        }

        return furrowed_entity.getFurrow().get().isWithinBounds(this.asServerPlayer().level, pos);
    }

    @ModifyExpressionValue(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getSharedSpawnPos()Lnet/minecraft/core/BlockPos;"
        ),

        method = "fudgeSpawnLocation"
    )
    private BlockPos furrowedSpawnPos(BlockPos original) {
        final var furrowed_entity = (FurrowedEntity) this;

        if (furrowed_entity.getFurrow().isEmpty()) {
            return original;
        }

        final var furrow = furrowed_entity.getFurrow().get();
        final var bounds = furrow.getBoundInfo(this.asServerPlayer().level);

        switch (bounds.axis()) {
            case X: return new BlockPos(Mth.floor(bounds.intercept()), original.getY(), original.getZ());
            case Z: return new BlockPos(original.getX(), original.getY(), Mth.floor(bounds.intercept()));

            default:
            case Y: return original;
        }
    }
}

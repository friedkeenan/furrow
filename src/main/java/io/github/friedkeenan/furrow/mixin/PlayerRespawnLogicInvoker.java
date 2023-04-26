package io.github.friedkeenan.furrow.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;

@Mixin(PlayerRespawnLogic.class)
public interface PlayerRespawnLogicInvoker {
    @Invoker
    @Nullable
    public static BlockPos callGetOverworldRespawnPos(ServerLevel level, int x, int z) {
        throw new AssertionError();
    }
}

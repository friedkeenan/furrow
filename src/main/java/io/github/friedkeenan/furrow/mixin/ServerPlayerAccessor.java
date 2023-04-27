package io.github.friedkeenan.furrow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
    @Invoker
    public void callFudgeSpawnLocation(ServerLevel level);
}

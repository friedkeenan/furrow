package io.github.friedkeenan.furrow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;

public class FurrowMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("furrow");

    @Override
    public void onInitialize() {
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation("furrow:furrow_type"),
            FurrowTypeArgument.class,
            SingletonArgumentInfo.contextFree(FurrowTypeArgument::furrowType)
        );

        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation("furrow:intercept"),
            InterceptArgument.class,
            SingletonArgumentInfo.contextFree(InterceptArgument::intercept)
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registry_access, environment) -> {
            FurrowCommand.register(dispatcher);
        });

        LOGGER.info("furrow initialized!");
    }
}

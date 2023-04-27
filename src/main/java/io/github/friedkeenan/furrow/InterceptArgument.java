package io.github.friedkeenan.furrow;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.util.Mth;

public class InterceptArgument implements ArgumentType<WorldCoordinate> {
	private InterceptArgument() { }

	public static InterceptArgument intercept() {
		return new InterceptArgument();
	}

	public static int getInt(CommandContext<CommandSourceStack> ctx, String name, Furrow.Type type) {
		final var source = ctx.getSource();

		final var unscaled = ctx.getArgument(name, WorldCoordinate.class).get(source.getPosition().get(type.intercept_axis));

		return Mth.floor(source.getLevel().dimensionType().coordinateScale() * unscaled);
	}

	@Override
	public WorldCoordinate parse(StringReader reader) throws CommandSyntaxException {
		return WorldCoordinate.parseInt(reader);
	}
}

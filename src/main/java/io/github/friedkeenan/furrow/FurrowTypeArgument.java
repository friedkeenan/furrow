package io.github.friedkeenan.furrow;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringRepresentableArgument;

public class FurrowTypeArgument extends StringRepresentableArgument<Furrow.Type> {
    private FurrowTypeArgument() {
        super(Furrow.Type.CODEC, Furrow.Type::values);
    }

    public static FurrowTypeArgument furrowType() {
        return new FurrowTypeArgument();
    }

    public static Furrow.Type getFurrowType(CommandContext<CommandSourceStack> ctx, String name) {
        return ctx.getArgument(name, Furrow.Type.class);
    }
}

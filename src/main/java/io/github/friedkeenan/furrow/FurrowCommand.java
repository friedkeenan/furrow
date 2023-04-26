package io.github.friedkeenan.furrow;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public class FurrowCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("furrow")
                /* Game Master. */
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("clear")
                    .executes(FurrowCommand::clearFurrowOfSource)

                    .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(FurrowCommand::clearFurrow)
                    )
                )

                .then(Commands.literal("set")
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("type", FurrowTypeArgument.furrowType())
                            .then(Commands.argument("intercept", InterceptArgument.intercept())
                                .executes(ctx -> setFurrowWithBreadth(ctx, 1))

                                .then(Commands.argument("breadth", IntegerArgumentType.integer(1))
                                    .executes(FurrowCommand::setFurrowWithBreadthArgument)
                                )
                            )
                        )
                    )
                )
        );
    }

    private static int clearFurrowOfSource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final var entity = ctx.getSource().getEntityOrException();

        ((FurrowedEntity) entity).clearFurrow();

        return 0;
    }

    private static int clearFurrow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        for (final var entity : EntityArgument.getEntities(ctx, "targets")) {
            ((FurrowedEntity) entity).clearFurrow();
        }

        return 0;
    }

    private static int setFurrowWithBreadthArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setFurrowWithBreadth(ctx, IntegerArgumentType.getInteger(ctx, "breadth"));
    }

    private static int setFurrowWithBreadth(CommandContext<CommandSourceStack> ctx, int breadth) throws CommandSyntaxException {
        final var type      = FurrowTypeArgument.getFurrowType(ctx, "type");
        final var intercept = InterceptArgument.getInt(ctx, "intercept", type);

        final var furrow = Optional.of(new Furrow(type, intercept, breadth));

        for (final var entity : EntityArgument.getEntities(ctx, "targets")) {
            ((FurrowedEntity) entity).setFurrow(furrow);
        }

        return 0;
    }
}

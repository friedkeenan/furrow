package io.github.friedkeenan.furrow;

import net.minecraft.world.entity.Entity;

public class SafeDismount {
    /*
        The game code which handles where to place
        a player when they leave a bed or respawn
        only cares about entity *types*, and not
        individual entities. Furrows however are
        entity-specific, and so in order to count
        areas outside an entity's furrow as unsafe,
        we track the relevant entity to later retrieve
        in the function that determines that safety.
    */
    public static final ThreadLocal<Entity> DISMOUNTING_ENTITY = new ThreadLocal<>();
}

<img src="src/main/resources/assets/furrow/icon.png" width="128">

# Furrow

Imprison entities to a single line of blocks!

This mod adds the ability to imprison an entity (namely a player) in a "furrow". A furrow can be a line of blocks along the X or Z axis (a vertical plane), or a horizontal plane which restricts you to a portion of the Y axis. When an entity has an associated furrow, the edges of that furrow will act much like a worldborder, with some notable differences, namely that the furrow can be reached through, and that different entities can have different furrows.

*Inspired by [WadZee](https://www.youtube.com/@WadZee)'s "Straight Line Only" videos.*

## The `/furrow` command

To manipulate an entity's furrow, the `/furrow` command is added with the following syntax:

- `/furrow clear [<targets>]`
    - Clears the furrows of the specified targets. If no targets are specified, then clears the furrow of the executing entity.
- `/furrow set <targets> <type> <intercept> [<breadth>]`
    - Sets the furrow of the specified targets.
    - The `type` argument can be one of `along_x`, `along_z`, and `horizontal`.
    - The `intercept` argument specifies where the furrow intersects with the corresponding axis.
        - For `along_x`, the `intercept` argument is a Z-coordinate.
        - For `along_z`, the `intercept` argument is an X-coordinate.
        - For `horizontal`, the `intercept` argument is a Y-coordinate.
    - The `breadth` argument specifies the distance from one edge of the furrow to the other. If unspecified, a value of `1` is used.

## Enforcement

In order to make sure that an entity does not leave their furrow, the following methods of enforcement are used:

- Most importantly, entities will collide with the edges of the furrow like they would if blocks were there.
- If an entity is not wholly contained within the furrow, then they will take suffocation damage.
- When not wholly contained within their furrow, a player is not able to interact with the world. This includes placing and breaking blocks, using items, interacting with entities, and collecting items.
- Beds may only be used if they overlap with a player's furrow, and when leaving a bed (or respawning at one), the player will be placed inside their furrow.
- Due to how vehicles work, they can be used to bring an entity outside of their furrow. However, if the hitbox of a passenger's vehicle does not overlap with the bounds of the passenger's furrow, then they will be dismounted. Accordingly, it is not possible to start riding a vehicle if it is not partially within the would-be passenger's furrow.
    - Additionally, since precisely controlling vehicles can be a bit difficult, some leniency is given to passengers. Instead of suffocating the moment they're not wholly contained within their furrow, they will instead start suffocating the moment they are wholly outside their furrow.

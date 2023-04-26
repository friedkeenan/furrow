package io.github.friedkeenan.furrow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Furrow {
    public static enum Type implements StringRepresentable {
        ALONG_X("along_x",       Direction.Axis.Z),
        ALONG_Z("along_z",       Direction.Axis.X),
        HORIZONTAL("horizontal", Direction.Axis.Y);

        public static final StringRepresentable.EnumCodec<Type> CODEC;

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }

        private final String name;

        public final Direction.Axis intercept_axis;

        private Type(String name, Direction.Axis intercept_axis) {
            this.name           = name;
            this.intercept_axis = intercept_axis;
        }

        public boolean needsScaling() {
            return this.intercept_axis.isHorizontal();
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Type byName(String string) {
            return CODEC.byName(string, ALONG_X);
        }
    }

    public static record BoundInfo(Direction.Axis axis, double intercept, double min, double max) {
        public BoundInfo extend(double extension) {
            return new BoundInfo(this.axis(), this.intercept(), this.min() - extension, this.max() + extension);
        }
    }

    private static final double EPSILON = 1.0E-6;

    private static final Vec3 NEGATIVE_INFINITY = new Vec3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    private static final Vec3 POSITIVE_INFINITY = new Vec3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    private static final String FURROW_TAG    = "Furrow";
    private static final String TYPE_TAG      = "Type";
    private static final String INTERCEPT_TAG = "Intercept";
    private static final String BREADTH_TAG   = "Breadth";

    private final Type type;
    private final int  intercept;
    private final int  breadth;

    private final VoxelShape shape;

    private final Map<Double, VoxelShape> scaled_shapes = new HashMap<Double, VoxelShape>();

    private double realIntercept(double scale) {
        var real_intercept = this.intercept;
        if (this.type.needsScaling()) {
            real_intercept /= scale;
        }

        if (this.breadth % 2 == 0) {
            return real_intercept;
        }

        return real_intercept + 0.5;
    }

    private VoxelShape makeShape(double scale) {
        final var real_intercept = this.realIntercept(scale);

        final var min = NEGATIVE_INFINITY.with(this.type.intercept_axis, real_intercept - this.breadth / 2.0);
        final var max = POSITIVE_INFINITY.with(this.type.intercept_axis, real_intercept + this.breadth / 2.0);

        return Shapes.join(
            Shapes.INFINITY,

            Shapes.box(
                min.x(), min.y(), min.z(),
                max.x(), max.y(), max.z()
            ),

            BooleanOp.ONLY_FIRST
        );
    }

    public Furrow(Type type, int intercept, int breadth) {
        this.type      = type;
        this.intercept = intercept;
        this.breadth   = breadth;

        this.shape = this.makeShape(1.0);
    }

    public static Optional<Furrow> readFromCompound(CompoundTag data) {
        if (!data.contains(FURROW_TAG)) {
            return Optional.empty();
        }

        final var furrow_data = data.getCompound(FURROW_TAG);

        final var type      = furrow_data.getString(TYPE_TAG);
        final var intercept = furrow_data.getInt(INTERCEPT_TAG);
        final var breadth   = furrow_data.getInt(BREADTH_TAG);

        return Optional.of(new Furrow(Type.byName(type), intercept, breadth));
    }

    public static Furrow readFromBuffer(FriendlyByteBuf buf) {
        final var type      = buf.readEnum(Type.class);
        final var intercept = buf.readVarInt();
        final var breadth   = buf.readVarInt();

        return new Furrow(type, intercept, breadth);
    }

    public void writeToCompound(CompoundTag data) {
        final var furrow_data = new CompoundTag();

        furrow_data.putString(TYPE_TAG,   this.type.getName());
        furrow_data.putInt(INTERCEPT_TAG, this.intercept);
        furrow_data.putInt(BREADTH_TAG,   this.breadth);

        data.put(FURROW_TAG, furrow_data);
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
        buf.writeVarInt(this.intercept);
        buf.writeVarInt(this.breadth);
    }

    public VoxelShape getCollisionShape(Level level) {
        if (!this.type.needsScaling()) {
            return this.shape;
        }

        final var scale = level.dimensionType().coordinateScale();

        if (scale == 1.0) {
            return this.shape;
        }

        return this.scaled_shapes.computeIfAbsent(scale, s -> this.makeShape(s));
    }

    public BoundInfo getBoundInfo(Level level) {
        final var real_intercept = this.realIntercept(level.dimensionType().coordinateScale());

        final var min = real_intercept - this.breadth / 2.0;
        final var max = real_intercept + this.breadth / 2.0;

        return new BoundInfo(type.intercept_axis, real_intercept, min, max);
    }

    public boolean isWithinBounds(Level level, AABB hitbox) {
        final var info = this.getBoundInfo(level);

        return hitbox.min(info.axis()) >= info.min() && hitbox.max(info.axis()) <= info.max();
    }

    public boolean isWithinBounds(Level level, BlockPos pos) {
        final var info = this.getBoundInfo(level);

        final var coord = pos.get(info.axis());

        return (coord + 1) > info.min() && coord < info.max();
    }

    public boolean overlapsWithBounds(Level level, AABB hitbox) {
        final var info = this.getBoundInfo(level);

        return hitbox.max(info.axis()) >= info.min() && hitbox.min(info.axis()) <= info.max();
    }

    private static AABB lenientBox(Entity entity) {
        if (entity.isPassenger()) {
            return AABB.ofSize(entity.getEyePosition(), entity.getBbWidth(), EPSILON, entity.getBbWidth());
        }

        return entity.getBoundingBox();
    }

    public boolean lenientIsWithinBounds(Level level, Entity entity) {
        return this.isWithinBounds(level, lenientBox(entity));
    }

    @Override
    public boolean equals(Object other_obj) {
        if (!(other_obj instanceof Furrow)) {
            return false;
        }

        final var other = (Furrow) other_obj;

        return (
            this.type      == other.type   &&
            this.intercept == other.intercept &&
            this.breadth   == other.breadth
        );
    }
}

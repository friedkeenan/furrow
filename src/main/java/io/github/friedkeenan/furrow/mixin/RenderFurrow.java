package io.github.friedkeenan.furrow.mixin;

import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;

import io.github.friedkeenan.furrow.Furrow;
import io.github.friedkeenan.furrow.FurrowedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
@Environment(EnvType.CLIENT)
public class RenderFurrow {
    private static final float MAIN_RED   = 1.0f;
    private static final float MAIN_GREEN = 48 / 255.0f;
    private static final float MAIN_BLUE  = 48 / 255.0f;

    private static final float VEHICLE_RED   = 32 / 255.0f;
    private static final float VEHICLE_GREEN = 160 / 255.0f;
    private static final float VEHICLE_BLUE  = 1.0f;

    @Shadow
    @Final
    private static ResourceLocation FORCEFIELD_LOCATION;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private double renderDistance(Direction.Axis axis) {
        if (axis.isVertical()) {
            return this.minecraft.gameRenderer.getDepthFar();
        }

        return this.minecraft.options.getEffectiveRenderDistance() * ChunkRenderDispatcher.RenderChunk.SIZE;
    }

    @Unique
    private static Pair<Direction.Axis, Direction.Axis> perpendicularAxes(Direction.Axis axis) {
        switch (axis) {
            case X:
                return Pair.of(Direction.Axis.Z, Direction.Axis.Y);

            case Y:
                return Pair.of(Direction.Axis.X, Direction.Axis.Z);

            case Z:
            default:
                return Pair.of(Direction.Axis.X, Direction.Axis.Y);
        }
    }

    @Unique
    private static VertexConsumer buildVertex(VertexConsumer builder, Vec3 vertex) {
        return builder.vertex(vertex.x(), vertex.y(), vertex.z());
    }

    @Unique
    private void renderFurrowBounds(
        Camera           camera,
        Furrow.BoundInfo bound_info,

        float red, float green, float blue,

        BiFunction<Double, Furrow.BoundInfo, Boolean> should_render,
        BiFunction<Double, Furrow.BoundInfo, Boolean> render_max
    ) {
        final var camera_bound_coord = camera.getPosition().get(bound_info.axis());

        if (!should_render.apply(camera_bound_coord, bound_info)) {
            return;
        }

        final var bound_render_distance = this.renderDistance(bound_info.axis());

        /* If the camera is too far to see the furrow border, don't render anything. */
        if (
            camera_bound_coord < bound_info.min() - bound_render_distance ||
            camera_bound_coord > bound_info.max() + bound_render_distance
        ) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
        RenderSystem.depthMask(Minecraft.useShaderTransparency());

        PoseStack pose_stack = RenderSystem.getModelViewStack();
        pose_stack.pushPose();

        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShaderColor(red, green, blue, 1.0f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.polygonOffset(-3.0f, -3.0f);
        RenderSystem.enablePolygonOffset();
        RenderSystem.disableCull();

        final var animation_proportion = (Util.getMillis() % 3000L) / 3000.0f;

        final var render_axes  = perpendicularAxes(bound_info.axis());
        final var step_axis    = render_axes.getFirst();
        final var distant_axis = render_axes.getSecond();

        final var camera_step_coord = camera.getPosition().get(step_axis);

        final var step_limit    = this.renderDistance(step_axis);
        final var distant_limit = this.renderDistance(distant_axis);

        final var min_step_coord = camera_step_coord - step_limit;
        final var max_step_coord = camera_step_coord + step_limit;

        final var bound_coord = render_max.apply(camera_bound_coord, bound_info) ? bound_info.max() : bound_info.min();
        // final var bound_coord = (camera_bound_coord > bound_info.max()) ? bound_info.max() : bound_info.min();

        final var builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        final var camera_height_offset  = (float) -Mth.frac(camera.getPosition().get(distant_axis) * 0.5);
        final var distant_height_offset = camera_height_offset + (float) distant_limit;

        var camera_width_offset = (Mth.floor(min_step_coord) % 2 == 0) ? 0.0f : 0.5f;
        for (var step_coord = min_step_coord; step_coord < max_step_coord; ++step_coord) {
            final var step               = Math.min(1.0, max_step_coord - step_coord);
            final var local_width_offset = 0.5f * (float) step;

            final var common = Vec3.ZERO.with(bound_info.axis(), bound_coord - camera_bound_coord);

            buildVertex(
                builder,
                common
                    .with(step_axis,    step_coord - camera_step_coord)
                    .with(distant_axis, -distant_limit)
            ).uv(animation_proportion - camera_width_offset, animation_proportion + distant_height_offset).endVertex();

            buildVertex(
                builder,

                common
                    .with(step_axis,    step_coord - camera_step_coord + step)
                    .with(distant_axis, -distant_limit)
            ).uv(animation_proportion - (camera_width_offset + local_width_offset), animation_proportion + distant_height_offset).endVertex();

            buildVertex(
                builder,

                common
                    .with(step_axis,    step_coord - camera_step_coord + step)
                    .with(distant_axis, distant_limit)
            ).uv(animation_proportion - (camera_width_offset + local_width_offset), animation_proportion + camera_height_offset).endVertex();

            buildVertex(
                builder,

                common
                    .with(step_axis,    step_coord - camera_step_coord)
                    .with(distant_axis, distant_limit)
            ).uv(animation_proportion - camera_width_offset, animation_proportion + camera_height_offset).endVertex();

            camera_width_offset += 0.5f;
        }

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.enableCull();
        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        pose_stack.popPose();

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
    }

    @Inject(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderWorldBorder(Lnet/minecraft/client/Camera;)V"
        ),

        method = "renderLevel"
    )
    private void renderFurrow(
        PoseStack pose_stack, float f, long l, boolean bl, Camera camera, GameRenderer game_renderer, LightTexture light_exture, Matrix4f matrix,

        CallbackInfo info
    ) {
        @Nullable final var player = this.minecraft.player;

        if (player == null) {
            return;
        }

        final var furrow = ((FurrowedEntity) player).getFurrow();
        if (furrow.isEmpty()) {
            return;
        }

        final var bound_info = furrow.get().getBoundInfo(this.minecraft.level);

        this.renderFurrowBounds(
            camera,
            bound_info,

            MAIN_RED, MAIN_GREEN, MAIN_BLUE,

            (camera_bound_coord, bounds) -> {
                return camera_bound_coord < bounds.min() || camera_bound_coord > bounds.max();
            },

            (camera_bound_coord, bounds) -> camera_bound_coord > bounds.max()
        );

        if (!this.minecraft.player.isPassenger()) {
            return;
        }

        final var vehicle     = this.minecraft.player.getVehicle();
        final var vehicle_box = vehicle.getBoundingBox();

        final var breadth = vehicle_box.max(bound_info.axis()) - vehicle_box.min(bound_info.axis());

        final var extended_bound_info = bound_info.extend(breadth);
        this.renderFurrowBounds(
            camera,
            extended_bound_info,

            VEHICLE_RED, VEHICLE_GREEN, VEHICLE_BLUE,

            (camera_bound_coord, bounds) -> {
                return camera_bound_coord < bound_info.min() || camera_bound_coord > bound_info.max();
            },

            (camera_bound_coord, bounds) -> camera_bound_coord > bound_info.max()
        );
    }

    @ModifyExpressionValue(
        at = @At(
            value  = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"
        ),

        method = "renderLevel"
    )
    private boolean noHitOutlineOutsideFurrow(boolean original) {
        if (original) {
            return true;
        }

        @Nullable final var player = this.minecraft.player;
        if (player == null) {
            return false;
        }

        final var furrowed_entity = (FurrowedEntity) player;
        if (furrowed_entity.getFurrow().isEmpty()) {
            return false;
        }

        return !furrowed_entity.getFurrow().get().lenientIsWithinBounds(player.level, player);
    }
}

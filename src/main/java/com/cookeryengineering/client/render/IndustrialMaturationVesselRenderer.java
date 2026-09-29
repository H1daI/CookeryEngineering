package com.cookeryengineering.client.render;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockOrientation;
import blusunrize.immersiveengineering.client.render.tile.BERenderUtils;
import blusunrize.immersiveengineering.client.render.tile.IEMultiblockRenderer;
import blusunrize.immersiveengineering.client.render.tile.MixerRenderer;
import com.cookeryengineering.content.multiblock.IndustrialMaturationVesselLogic.State;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

/** 渲染 IE 搅拌器的搅拌桨，但因为盖子的原因实际上作为用户应该看不到内部的动画，考虑以后的更改暂时保留*/
public final class IndustrialMaturationVesselRenderer extends IEMultiblockRenderer<State> {
    @Override
    public void render(
            IMultiblockContext<State> context,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        State state = context.getState();
        MultiblockOrientation orientation = context.getLevel().getOrientation();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        buffers = BERenderUtils.mirror(orientation, poseStack, buffers);

        Direction front = orientation.front();
        poseStack.translate(
                front == Direction.SOUTH || front == Direction.WEST ? -0.5 : 0.5,
                0,
                front == Direction.SOUTH || front == Direction.EAST ? 0.5 : -0.5
        );
        float angle = state.getAnimationAgitator()
                - (!state.isActive() ? 0 : (1 - partialTicks) * 9F);
        poseStack.mulPose(new Quaternionf().rotateY(angle * Mth.DEG_TO_RAD));
        poseStack.translate(-0.5, -0.5, -0.5);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffers.getBuffer(RenderType.solid()),
                null,
                MixerRenderer.AGITATOR.get(),
                1, 1, 1,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.solid()
        );
        poseStack.popPose();
    }
}

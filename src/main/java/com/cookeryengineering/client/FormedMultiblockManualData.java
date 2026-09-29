package com.cookeryengineering.client;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks.MultiblockManualData;
import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;

/** 对沉浸原油projector的兼容。 */
public final class FormedMultiblockManualData implements MultiblockManualData {
    private final IETemplateMultiblock multiblock;
    private final ItemStack renderStack;
    private NonNullList<ItemStack> materials;

    public FormedMultiblockManualData(IETemplateMultiblock multiblock) {
        this.multiblock = multiblock;
        this.renderStack = new ItemStack(multiblock.getBlock());
    }

    @Override
    public NonNullList<ItemStack> getTotalMaterials() {
        if (materials == null) {
            Level level = Minecraft.getInstance().level;
            // 材料清单统计，缓存了就重新复用
            if (level == null) {
                return NonNullList.create();
            }
            materials = NonNullList.create();
            for (StructureBlockInfo info : multiblock.getStructure(level)) {
                if (info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE)
                        && info.state().getValue(IEProperties.MULTIBLOCKSLAVE)) {
                    continue;
                }
                ItemStack picked = Utils.getPickBlock(info.state());
                if (picked.isEmpty()) {
                    continue;
                }
                ItemStack existing = materials.stream()
                        .filter(stack -> ItemStack.isSameItem(stack, picked))
                        .findFirst()
                        .orElse(ItemStack.EMPTY);
                if (existing.isEmpty()) {
                    materials.add(picked.copy());
                } else {
                    existing.grow(1);
                }
            }
        }
        return materials;
    }

    @Override
    public boolean canRenderFormedStructure() {
        return true;
    }

    @Override
    public void renderFormedStructure(PoseStack transform, MultiBufferSource bufferSource) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(renderStack, null, null, 0);
        VertexConsumer buffer = bufferSource.getBuffer(IERenderTypes.TRANSLUCENT_FULLBRIGHT);
        RandomSource random = RandomSource.create(42L);

        transform.pushPose();
        BlockPos offset = multiblock.getMasterFromOriginOffset();
        transform.translate(offset.getX(), offset.getY(), offset.getZ());
        for (Direction direction : Direction.values()) {
            emitQuads(model, direction, random, transform, buffer);
        }
        emitQuads(model, null, random, transform, buffer);
        transform.popPose();
    }

    private static void emitQuads(
            BakedModel model,
            @Nullable Direction direction,
            RandomSource random,
            PoseStack transform,
            VertexConsumer buffer
    ) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, direction, random, ModelData.EMPTY, null)) {
            buffer.putBulkData(
                    transform.last(), quad, 1, 1, 1, 1,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
            );
        }
    }
}

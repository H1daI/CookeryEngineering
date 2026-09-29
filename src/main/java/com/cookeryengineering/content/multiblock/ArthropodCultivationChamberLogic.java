package com.cookeryengineering.content.multiblock;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;

public final class ArthropodCultivationChamberLogic
        implements IMultiblockLogic<ArthropodCultivationChamberLogic.State> {
    @Override
    public State createInitialState(IInitialMultiblockContext<State> context) {
        return new State();
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType shapeType) {
        return ignored -> Shapes.block();
    }

    public static final class State implements IMultiblockState {
        @Override
        public void writeSaveNBT(CompoundTag tag, HolderLookup.Provider provider) {
        }

        @Override
        public void readSaveNBT(CompoundTag tag, HolderLookup.Provider provider) {
        }
    }
}

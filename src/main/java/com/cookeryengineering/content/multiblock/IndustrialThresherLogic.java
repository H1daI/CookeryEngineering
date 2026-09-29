package com.cookeryengineering.content.multiblock;

import com.cookeryengineering.compat.kaleidoscope.KaleidoscopeMillstoneRecipes;
import com.cookeryengineering.content.processing.ThresherProcessor;
import blusunrize.immersiveengineering.common.util.DroppingMultiblockOutput;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.sound.MultiblockSound;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BooleanSupplier;

/** 从入口处接受散落物品输入 */
public final class IndustrialThresherLogic implements IMultiblockLogic<IndustrialThresherLogic.State>,
        IServerTickableComponent<IndustrialThresherLogic.State>,
        IClientTickableComponent<IndustrialThresherLogic.State> {
    private static final CapabilityPosition ENERGY = new CapabilityPosition(
            IndustrialThresherMultiblock.ENERGY_PORT, RelativeBlockFace.UP);
    private static final CapabilityPosition INPUT = new CapabilityPosition(
            IndustrialThresherMultiblock.INPUT_PORT, RelativeBlockFace.UP);
    private static final MultiblockFace OUTPUT_FACE = new MultiblockFace(
            RelativeBlockFace.FRONT, IndustrialThresherMultiblock.OUTPUT_PORT.offset(0, 0, 1));
    private static final AABB INTAKE = new AABB(2, 2, 1, 3, 3, 2);

    @Override
    public State createInitialState(IInitialMultiblockContext<State> context) {
        return new State(context);
    }

    @Override
    public void registerCapabilities(CapabilityRegistrar<State> register) {
        register.register(Capabilities.EnergyStorage.BLOCK, (state, position) ->
                ENERGY.equals(position) ? state.processor.energy : null);
        register.register(Capabilities.ItemHandler.BLOCK, (state, position) -> {
            if (INPUT.equals(position)) return state.inputPort;
            return null;
        });
    }

    @Override
    public void tickServer(IMultiblockContext<State> context) {
        State state = context.getState();
        boolean disabled = context.getRedstoneInputValue(IndustrialThresherMultiblock.REDSTONE_PORT, 0) > 0;
        if (disabled != state.redstoneDisabled) {
            state.redstoneDisabled = disabled;
            context.markMasterDirty();
        }
        Consumer<ItemStack> output = stack -> state.output.insertOrDrop(stack, context.getLevel());
        state.processor.flushCompleted(output);
        if (!disabled) {
            AABB area = context.getLevel().toAbsolute(INTAKE);
            for (ItemEntity entity : context.getLevel().getRawLevel()
                    .getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
                ItemStack offered = entity.getItem();
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(state.inputPort, offered, false);
                if (remainder.getCount() == offered.getCount()) continue;
                if (remainder.isEmpty()) entity.discard();
                else entity.setItem(remainder);
            }
        }
        boolean activeThisTick = false;
        if (!disabled) {
            var level = context.getLevel().getRawLevel();
            activeThisTick = state.processor.tick(
                    ingredient -> state.recipes.find(level, ingredient), level.random, output);
        }
        if (state.active != activeThisTick) {
            state.active = activeThisTick;
            context.requestMasterBESync();
        }
        state.processor.energy.updateAverage();
        if (state.lastEnergy != state.processor.energy.getEnergyStored()) {
            state.lastEnergy = state.processor.energy.getEnergyStored();
            context.markMasterDirty();
        }
    }

    @Override
    public void tickClient(IMultiblockContext<State> context) {
        State state = context.getState();
        if (!state.isSoundPlaying.getAsBoolean()) {
            Vec3 soundPos = context.getLevel().toAbsolute(new Vec3(2, 1.5, 2));
            state.isSoundPlaying = MultiblockSound.startSound(
                    () -> state.active, context.isValid(), soundPos, IESounds.crusher, 0.5F);
        }
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType type) {
        return ignored -> Shapes.block();
    }

    @Override
    public void dropExtraItems(State state, Consumer<ItemStack> drop) {
        state.processor.dropStoredItems(drop);
    }

    public static final class State implements IMultiblockState {
        private final ThresherProcessor processor;
        private final KaleidoscopeMillstoneRecipes recipes = new KaleidoscopeMillstoneRecipes();
        private final IItemHandler inputPort;
        private final DroppingMultiblockOutput output;
        private boolean redstoneDisabled;
        private boolean active;
        private int lastEnergy;
        private BooleanSupplier isSoundPlaying = () -> false;

        private State(IInitialMultiblockContext<State> context) {
            processor = new ThresherProcessor(context.getMarkDirtyRunnable());
            inputPort = new WrappingItemHandler(processor.inputs, true, false);
            output = new DroppingMultiblockOutput(OUTPUT_FACE, context);
        }

        @Override
        public void writeSaveNBT(CompoundTag tag, HolderLookup.Provider provider) {
            processor.save(tag, provider);
        }

        @Override
        public void readSaveNBT(CompoundTag tag, HolderLookup.Provider provider) {
            processor.load(tag, provider);
        }

        @Override
        public void writeSyncNBT(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("active", active);
        }

        @Override
        public void readSyncNBT(CompoundTag tag, HolderLookup.Provider provider) {
            active = tag.getBoolean("active");
        }
    }
}

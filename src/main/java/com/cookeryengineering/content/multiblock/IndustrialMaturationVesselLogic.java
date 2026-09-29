package com.cookeryengineering.content.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.cookeryengineering.compat.kaleidoscope.KaleidoscopeStockpotRecipes;
import com.cookeryengineering.content.menu.IndustrialMaturationVesselMenu;

import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
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
import blusunrize.immersiveengineering.common.fluids.ArrayFluidHandler;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler;
import blusunrize.immersiveengineering.common.util.sound.MultiblockSound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 工业熟化釜的服务端逻辑。
 *
 * <p>服务端 tick 会检测机器红石端口
 * 处理过程受能量限制，客户端套用 IE 搅拌器的动画和运行声音</p>
 */
public final class IndustrialMaturationVesselLogic
        implements IMultiblockLogic<IndustrialMaturationVesselLogic.State>,
        IServerTickableComponent<IndustrialMaturationVesselLogic.State>,
        IClientTickableComponent<IndustrialMaturationVesselLogic.State> {

    private static final CapabilityPosition FLUID_INPUT = new CapabilityPosition(
            IndustrialMaturationVesselMultiblock.FLUID_INPUT_PORT, RelativeBlockFace.RIGHT);
    private static final CapabilityPosition ENERGY_INPUT = new CapabilityPosition(
            IndustrialMaturationVesselMultiblock.ENERGY_INPUT_PORT, RelativeBlockFace.UP);
    private static final CapabilityPosition CONTAINER_INPUT = new CapabilityPosition(
            IndustrialMaturationVesselMultiblock.CONTAINER_AND_OUTPUT_PORT, RelativeBlockFace.UP);
    private static final CapabilityPosition ITEM_OUTPUT = new CapabilityPosition(
            IndustrialMaturationVesselMultiblock.CONTAINER_AND_OUTPUT_PORT, RelativeBlockFace.BACK);
    private static final MultiblockFace ITEM_OUTPUT_FACE = new MultiblockFace(
            RelativeBlockFace.BACK, IndustrialMaturationVesselMultiblock.CONTAINER_AND_OUTPUT_PORT);

    @Override
    public State createInitialState(IInitialMultiblockContext<State> context) {
        return new State(context);
    }

    @Override
    public void registerCapabilities(CapabilityRegistrar<State> register) {
        register.registerAtOrNull(EnergyStorage.BLOCK, ENERGY_INPUT, state -> state.energy);
        register.register(FluidHandler.BLOCK, (state, position) ->
                FLUID_INPUT.equalsOrNullFace(position) ? state.fluidInput : null);
        register.register(ItemHandler.BLOCK, (state, position) -> {
            if (IndustrialMaturationVesselMultiblock.ITEM_INPUT_PORTS.contains(position.posInMultiblock())) {
                return state.itemInput;
            }
            if (CONTAINER_INPUT.equalsOrNullFace(position)) {
                return state.containerInput;
            }
            if (ITEM_OUTPUT.equals(position)) {
                return state.itemOutput;
            }
            return null;
        });
    }

    @Override
    public void dropExtraItems(State state, Consumer<ItemStack> drop) {
        state.dropStoredItems(drop);
    }

    @Override
    public ItemInteractionResult click(
            IMultiblockContext<State> context,
            BlockPos posInMultiblock,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            boolean isClient
    ) {
        if (!isClient) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> IndustrialMaturationVesselMenu.makeServer(
                            id, inventory, context.getState(), context.isValid()),
                    Component.translatable("block.cookery_engineering.industrial_maturation_vessel")
            ));
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType shapeType) {
        return ignored -> Shapes.block();
    }

    @Override
    public void tickServer(IMultiblockContext<State> context) {
        State state = context.getState();
        boolean powered = context.getRedstoneInputValue(
                IndustrialMaturationVesselMultiblock.REDSTONE_PORT, 0) > 0;
        boolean risingEdge = powered && !state.redstonePowered;
        if (powered != state.redstonePowered) {
            state.redstonePowered = powered;
            context.markMasterDirty();
        }
        if (risingEdge) {
            onRedstonePulse(context);
        }

        boolean activeThisTick = state.tickProcess();
        if (activeThisTick) {
            context.markMasterDirty();
        }
        if (state.tryServeFinishedBatch(context.getLevel().getRawLevel())) {
            context.markMasterDirty();
        }
        if (state.pushBufferedOutputs()) {
            context.markMasterDirty();
        }
        if (state.active != activeThisTick) {
            state.active = activeThisTick;
            context.markMasterDirty();
            context.requestMasterBESync();
        }
        state.energy.updateAverage();
    }

    @Override
    public void tickClient(IMultiblockContext<State> context) {
        State state = context.getState();
        if (!state.active) {
            return;
        }
        state.animationAgitator = (state.animationAgitator + 9) % 360;
        if (!state.isSoundPlaying.getAsBoolean()) {
            Vec3 soundPos = context.getLevel().toAbsolute(new Vec3(1.5, 1.5, 1.5));
            state.isSoundPlaying = MultiblockSound.startSound(
                    () -> state.active, context.isValid(), soundPos, IESounds.mixer, 0.075F);
        }
    }

    /**
     * 脉冲入口
     */
    public boolean onRedstonePulse(IMultiblockContext<State> context) {
        State state = context.getState();
        ResourceLocation previousSoupBase = state.soupBase;
        boolean started = state.tryStartBatch(context.getLevel().getRawLevel());
        if (started || !Objects.equals(previousSoupBase, state.soupBase)) {
            context.markDirtyAndSync();
        }
        return started;
    }

    public static final class State implements IMultiblockState {
        public static final int INPUT_SLOTS = 9;
        public static final int MAX_BATCH_INPUTS = 6;
        public static final int OUTPUT_SLOTS = 12;
        public static final int TANK_CAPACITY = 8_000;
        public static final int SOUP_BASE_AMOUNT = 1_000;
        public static final int DEFAULT_PROCESS_TICKS = 60;
        public static final int ENERGY_CAPACITY = 1_024;
        public static final int ENERGY_PER_TICK = 32;

        private static final String INPUTS_NBT = "inputs";
        private static final String CONTAINER_NBT = "container";
        private static final String OUTPUTS_NBT = "outputs";
        private static final String TANK_NBT = "tank";
        private static final String SOUP_BASE_NBT = "soup_base";
        private static final String SOUP_BASE_FROM_TANK_NBT = "soup_base_from_tank";
        private static final String PROCESS_TICKS_NBT = "process_ticks";
        private static final String PROCESS_TICKS_TOTAL_NBT = "process_ticks_total";
        private static final String PENDING_RESULT_NBT = "pending_result";
        private static final String CACHED_RECIPE_NBT = "cached_recipe";
        private static final String CACHED_RECIPE_FLEX_NBT = "cached_recipe_flex";
        private static final String CACHED_RICE_BOWL_RECIPE_NBT = "cached_rice_bowl_recipe";
        private static final String REDSTONE_POWERED_NBT = "redstone_powered";
        private static final String ENERGY_NBT = "energy";

        private final ItemStackHandler inputs;
        private final ItemStackHandler container;
        private final ItemStackHandler outputs;
        private final FluidTank tank;
        private final AveragingEnergyStorage energy = new AveragingEnergyStorage(ENERGY_CAPACITY);
        private final IItemHandler itemInput;
        private final IItemHandler containerInput;
        private final IItemHandler itemOutput;
        private final IFluidHandler fluidInput;
        private final Supplier<@Nullable IItemHandler> itemOutputTarget;
        private final Runnable markDirty;

        private @Nullable ResourceLocation soupBase;
        private boolean soupBaseFromTank;
        private boolean redstonePowered;
        private int processTicks;
        private int processTicksTotal;
        private ItemStack pendingResult = ItemStack.EMPTY;
        private @Nullable KaleidoscopeStockpotRecipes.CachedRecipe cachedRecipe;
        private @Nullable Ingredient batchCarrier;
        private @Nullable ResourceLocation cachedRiceBowlRecipe;

        // 初始化
        public boolean active;
        public float animationAgitator;
        private BooleanSupplier isSoundPlaying = () -> false;

        private State(IInitialMultiblockContext<State> context) {
            this.markDirty = context.getMarkDirtyRunnable();
            this.inputs = new ItemStackHandler(INPUT_SLOTS) {
                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }

                @Override
                protected void onContentsChanged(int slot) {
                    markDirty.run();
                }
            };
            this.container = dirtyingItemHandler(1, markDirty);
            this.outputs = dirtyingItemHandler(OUTPUT_SLOTS, markDirty);
            this.tank = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    markDirty.run();
                }
            };
            this.itemInput = new SoupBaseAwareInputHandler();
            this.containerInput = new WrappingItemHandler(container, true, false);
            this.itemOutput = new WrappingItemHandler(outputs, false, true);
            this.fluidInput = ArrayFluidHandler.fillOnly(tank, markDirty);
            this.itemOutputTarget = context.getCapabilityAt(ItemHandler.BLOCK, ITEM_OUTPUT_FACE);
        }

        private static ItemStackHandler dirtyingItemHandler(int slots, Runnable markDirty) {
            return new ItemStackHandler(slots) {
                @Override
                protected void onContentsChanged(int slot) {
                    markDirty.run();
                }
            };
        }

        /**
         * 接收到红石脉冲，启动一个批次
         */
        public boolean tryStartBatch(Level level) {
            if (level.isClientSide || hasBatch()) {
                return false;
            }

            if (soupBase == null && !detectSoupBaseFromTank()) {
                return false;
            }
            if (soupBaseFromTank && tank.getFluidAmount() < SOUP_BASE_AMOUNT) {
                return false;
            }

            BatchInputs batch = collectBatchInputs();
            if (batch.slots().isEmpty()) {
                return false;
            }

            KaleidoscopeStockpotRecipes.Match match = KaleidoscopeStockpotRecipes.find(
                    level, batch.recipeInputs(), Objects.requireNonNull(soupBase), cachedRecipe);
            if (match == null || match.result().isEmpty()) {
                return false;
            }

            if (soupBaseFromTank) {
                tank.drain(SOUP_BASE_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
            }
            for (int slot : batch.slots()) {
                inputs.extractItem(slot, 1, false);
            }

            cachedRecipe = match.recipe();
            batchCarrier = match.carrier();
            pendingResult = match.result().copy();
            processTicks = DEFAULT_PROCESS_TICKS;
            processTicksTotal = DEFAULT_PROCESS_TICKS;
            clearSoupBase();
            markDirty.run();
            return true;
        }

        private boolean detectSoupBaseFromTank() {
            FluidStack fluid = tank.getFluid();
            if (fluid.getAmount() < SOUP_BASE_AMOUNT) {
                return false;
            }
            ResourceLocation detected = KaleidoscopeStockpotRecipes.soupBaseFor(fluid);
            if (detected == null) {
                return false;
            }
            soupBase = detected;
            soupBaseFromTank = true;
            return true;
        }

        private BatchInputs collectBatchInputs() {
            NonNullList<ItemStack> recipeInputs = NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);
            List<Integer> selectedSlots = new ArrayList<>(MAX_BATCH_INPUTS);
            int recipeIndex = 0;
            for (int slot = 0; slot < inputs.getSlots() && recipeIndex < MAX_BATCH_INPUTS; slot++) {
                ItemStack stack = inputs.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    recipeInputs.set(recipeIndex++, stack.copyWithCount(1));
                    selectedSlots.add(slot);
                }
            }
            return new BatchInputs(recipeInputs, selectedSlots);
        }

        private boolean tickProcess() {
            if (pendingResult.isEmpty() || processTicks <= 0) {
                return false;
            }
            if (energy.extractEnergy(ENERGY_PER_TICK, true) < ENERGY_PER_TICK) {
                return false;
            }
            energy.extractEnergy(ENERGY_PER_TICK, false);
            processTicks--;
            return true;
        }

        private boolean tryServeFinishedBatch(Level level) {
            if (pendingResult.isEmpty() || processTicks > 0) {
                return false;
            }

            ItemStack suppliedContainer = container.getStackInSlot(0);
            ItemStack serving;
            boolean consumeContainer;

            if (!suppliedContainer.isEmpty()
                    && KaleidoscopeStockpotRecipes.isCookedRice(suppliedContainer)) {
                KaleidoscopeStockpotRecipes.RiceBowlMatch riceBowl =
                        KaleidoscopeStockpotRecipes.findRiceBowl(
                                level, pendingResult, suppliedContainer, cachedRiceBowlRecipe);
                if (riceBowl == null || riceBowl.result().isEmpty()) {
                    return false;
                }
                cachedRiceBowlRecipe = riceBowl.recipeId();
                serving = riceBowl.result();
                consumeContainer = true;
            } else {
                Ingredient carrier = batchCarrier;
                if (carrier == null && cachedRecipe != null) {
                    carrier = KaleidoscopeStockpotRecipes.resolveCarrier(level, cachedRecipe);
                    batchCarrier = carrier;
                }
                if (carrier == null) {
                    return false;
                }
                if (!carrier.isEmpty()) {
                    if (suppliedContainer.isEmpty() || !carrier.test(suppliedContainer)) {
                        return false;
                    }
                    consumeContainer = true;
                } else {
                    consumeContainer = false;
                }
                serving = pendingResult.copyWithCount(1);
            }

            if (!ItemHandlerHelper.insertItemStacked(outputs, serving, true).isEmpty()) {
                return false;
            }

            ItemHandlerHelper.insertItemStacked(outputs, serving, false);
            if (consumeContainer) {
                container.extractItem(0, 1, false);
            }
            pendingResult.shrink(1);
            if (pendingResult.isEmpty()) {
                pendingResult = ItemStack.EMPTY;
                processTicksTotal = 0;
                batchCarrier = null;
            }
            return true;
        }

        public boolean setSoupBase(ResourceLocation soupBase) {
            if (this.soupBase != null || hasBatch()) {
                return false;
            }
            this.soupBase = soupBase;
            this.soupBaseFromTank = false;
            return true;
        }

        private ItemStack insertIngredientOrSoupBase(int slot, ItemStack stack, boolean simulate) {
            ResourceLocation bucketBase = KaleidoscopeStockpotRecipes.soupBaseBucketFor(stack);
            if (bucketBase == null) {
                return inputs.insertItem(slot, stack, simulate);
            }
            if (soupBase != null || hasBatch()
                    || !ItemHandlerHelper.insertItemStacked(outputs, new ItemStack(Items.BUCKET), true).isEmpty()) {
                return stack;
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            if (!simulate) {
                soupBase = bucketBase;
                soupBaseFromTank = false;
                ItemHandlerHelper.insertItemStacked(outputs, new ItemStack(Items.BUCKET), false);
                markDirty.run();
            }
            return remainder;
        }

        private void clearSoupBase() {
            soupBase = null;
            soupBaseFromTank = false;
        }

        public boolean isProcessing() {
            return !pendingResult.isEmpty() && processTicks > 0;
        }

        public boolean hasBatch() {
            return !pendingResult.isEmpty();
        }

        public ItemStackHandler getInputs() {
            return inputs;
        }

        public ItemStackHandler getContainer() {
            return container;
        }

        public ItemStackHandler getOutputs() {
            return outputs;
        }

        private boolean pushBufferedOutputs() {
            IItemHandler destination = itemOutputTarget.get();
            return destination != null && pushOutputsInto(destination);
        }

        /**
         * 有容器自动推到容器中，没有就存在输出缓存里，IE中大部分机器的弹出方案
         */
        public boolean pushOutputsInto(IItemHandler destination) {
            if (destination == outputs) {
                return false;
            }
            boolean movedAny = false;
            for (int slot = 0; slot < outputs.getSlots(); slot++) {
                ItemStack stored = outputs.getStackInSlot(slot);
                if (stored.isEmpty()) {
                    continue;
                }
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                        destination, stored.copy(), false);
                int moved = stored.getCount() - remainder.getCount();
                if (moved > 0) {
                    outputs.extractItem(slot, moved, false);
                    movedAny = true;
                }
            }
            return movedAny;
        }

        private void dropStoredItems(Consumer<ItemStack> drop) {
            dropInventory(inputs, drop);
            dropInventory(container, drop);
            dropInventory(outputs, drop);
            if (!pendingResult.isEmpty()) {
                drop.accept(pendingResult);
                pendingResult = ItemStack.EMPTY;
                processTicks = 0;
                processTicksTotal = 0;
                batchCarrier = null;
            }
        }

        private static void dropInventory(ItemStackHandler inventory, Consumer<ItemStack> drop) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    drop.accept(stack);
                }
            }
        }

        public FluidTank getTank() {
            return tank;
        }

        public AveragingEnergyStorage getEnergy() {
            return energy;
        }

        public boolean isActive() {
            return active;
        }

        public float getAnimationAgitator() {
            return animationAgitator;
        }

        public @Nullable ResourceLocation getSoupBase() {
            return soupBase;
        }

        public int getProcessTicks() {
            return processTicks;
        }

        public int getProcessTicksTotal() {
            return processTicksTotal;
        }

        @Override
        public void writeSaveNBT(CompoundTag tag, HolderLookup.Provider provider) {
            tag.put(INPUTS_NBT, inputs.serializeNBT(provider));
            tag.put(CONTAINER_NBT, container.serializeNBT(provider));
            tag.put(OUTPUTS_NBT, outputs.serializeNBT(provider));
            tag.put(TANK_NBT, tank.writeToNBT(provider, new CompoundTag()));
            if (soupBase != null) {
                tag.putString(SOUP_BASE_NBT, soupBase.toString());
            }
            tag.putBoolean(SOUP_BASE_FROM_TANK_NBT, soupBaseFromTank);
            tag.putInt(PROCESS_TICKS_NBT, processTicks);
            tag.putInt(PROCESS_TICKS_TOTAL_NBT, processTicksTotal);
            tag.put(PENDING_RESULT_NBT, pendingResult.saveOptional(provider));
            if (cachedRecipe != null) {
                tag.putString(CACHED_RECIPE_NBT, cachedRecipe.id().toString());
                tag.putBoolean(CACHED_RECIPE_FLEX_NBT, cachedRecipe.flex());
            }
            if (cachedRiceBowlRecipe != null) {
                tag.putString(CACHED_RICE_BOWL_RECIPE_NBT, cachedRiceBowlRecipe.toString());
            }
            tag.putBoolean(REDSTONE_POWERED_NBT, redstonePowered);
            tag.put(ENERGY_NBT, energy.serializeNBT(provider));
        }

        @Override
        public void readSaveNBT(CompoundTag tag, HolderLookup.Provider provider) {
            inputs.deserializeNBT(provider, tag.getCompound(INPUTS_NBT));
            if (tag.contains(CONTAINER_NBT)) {
                container.deserializeNBT(provider, tag.getCompound(CONTAINER_NBT));
            }
            outputs.deserializeNBT(provider, tag.getCompound(OUTPUTS_NBT));
            tank.readFromNBT(provider, tag.getCompound(TANK_NBT));

            soupBase = ResourceLocation.tryParse(tag.getString(SOUP_BASE_NBT));
            soupBaseFromTank = soupBase != null && tag.getBoolean(SOUP_BASE_FROM_TANK_NBT);
            processTicks = Math.max(0, tag.getInt(PROCESS_TICKS_NBT));
            processTicksTotal = Math.max(processTicks, tag.getInt(PROCESS_TICKS_TOTAL_NBT));
            pendingResult = ItemStack.parseOptional(provider, tag.getCompound(PENDING_RESULT_NBT));

            ResourceLocation cachedId = ResourceLocation.tryParse(tag.getString(CACHED_RECIPE_NBT));
            cachedRecipe = cachedId == null ? null : new KaleidoscopeStockpotRecipes.CachedRecipe(
                    cachedId, tag.getBoolean(CACHED_RECIPE_FLEX_NBT));
            cachedRiceBowlRecipe = ResourceLocation.tryParse(tag.getString(CACHED_RICE_BOWL_RECIPE_NBT));
            batchCarrier = null;
            redstonePowered = tag.getBoolean(REDSTONE_POWERED_NBT);
            if (tag.contains(ENERGY_NBT)) {
                energy.deserializeNBT(provider, tag.get(ENERGY_NBT));
            }

            if (pendingResult.isEmpty()) {
                processTicks = 0;
                processTicksTotal = 0;
            }
        }

        @Override
        public void writeSyncNBT(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("active", active);
            tag.putFloat("animation_agitator", animationAgitator);
            tag.putInt(PROCESS_TICKS_NBT, processTicks);
            tag.putInt(PROCESS_TICKS_TOTAL_NBT, processTicksTotal);
            tag.putInt("energy_stored", energy.getEnergyStored());
            tag.put(TANK_NBT, tank.writeToNBT(provider, new CompoundTag()));
        }

        @Override
        public void readSyncNBT(CompoundTag tag, HolderLookup.Provider provider) {
            active = tag.getBoolean("active");
            animationAgitator = tag.getFloat("animation_agitator");
            processTicks = tag.getInt(PROCESS_TICKS_NBT);
            processTicksTotal = tag.getInt(PROCESS_TICKS_TOTAL_NBT);
            energy.setStoredEnergy(tag.getInt("energy_stored"));
            tank.readFromNBT(provider, tag.getCompound(TANK_NBT));
        }

        private final class SoupBaseAwareInputHandler implements IItemHandler {
            @Override
            public int getSlots() {
                return inputs.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inputs.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return insertIngredientOrSoupBase(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return inputs.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return KaleidoscopeStockpotRecipes.soupBaseBucketFor(stack) != null
                        || inputs.isItemValid(slot, stack);
            }
        }

        private record BatchInputs(NonNullList<ItemStack> recipeInputs, List<Integer> slots) {
        }
    }
}

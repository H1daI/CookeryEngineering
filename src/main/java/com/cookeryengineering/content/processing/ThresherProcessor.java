package com.cookeryengineering.content.processing;

import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
import com.cookeryengineering.compat.kaleidoscope.KaleidoscopeMillstoneRecipes.Output;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ThresherProcessor {
    public static final int SLOTS = 8;
    public static final int BATCH_LIMIT = 8;
    public static final int PROCESS_TICKS = 20;
    public static final int ENERGY_PER_TICK = 32;
    public static final int ENERGY_CAPACITY = 2048;
    public final AveragingEnergyStorage energy = new AveragingEnergyStorage(ENERGY_CAPACITY);
    public final ItemStackHandler inputs;
    private final Runnable dirty;
    private ItemStack batchInput = ItemStack.EMPTY;
    private final List<ItemStack> pendingResults = new ArrayList<>();
    private int remainingTicks;

    public ThresherProcessor(Runnable dirty) {
        this.dirty = dirty;
        inputs = inventory(dirty);
    }

    private static ItemStackHandler inventory(Runnable dirty) {
        return new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) { dirty.run(); }
        };
    }

    /** 配方为 null 表示直接通过。*/
    public boolean tick(Function<ItemStack, List<Output>> findRecipe, RandomSource random,
                        Consumer<ItemStack> output) {
        flushCompleted(output);
        if (batchInput.isEmpty()) {
            int first = -1;
            for (int i = 0; i < SLOTS; i++) {
                if (!inputs.getStackInSlot(i).isEmpty()) { first = i; break; }
            }
            if (first < 0) return false;
            ItemStack sample = inputs.getStackInSlot(first).copyWithCount(1);
            List<Output> recipe = findRecipe.apply(sample);
            if (recipe != null && energy.getEnergyStored() < ENERGY_PER_TICK) return false;

            int count = 0;
            for (int i = first; i < SLOTS && count < BATCH_LIMIT; i++) {
                if (ItemStack.isSameItemSameComponents(sample, inputs.getStackInSlot(i))) {
                    count += inputs.extractItem(i, BATCH_LIMIT - count, false).getCount();
                }
            }
            batchInput = sample.copyWithCount(count);
            if (recipe == null) {
                pendingResults.add(batchInput.copy());
                flushCompleted(output);
                dirty.run();
                return false;
            }
            // 每个输入和每个输出各独立抽取一次，并保留 KC 的堆叠数量和组件。
            for (int i = 0; i < count; i++) {
                for (Output result : recipe) {
                    if (!result.stack().isEmpty() && random.nextDouble() < result.chance()) {
                        pendingResults.add(result.stack().copy());
                    }
                }
            }
            remainingTicks = PROCESS_TICKS;
            dirty.run();
        }
        if (energy.extractEnergy(ENERGY_PER_TICK, true) < ENERGY_PER_TICK) return false;
        energy.extractEnergy(ENERGY_PER_TICK, false);
        remainingTicks--;
        if (remainingTicks == 0) flushCompleted(output);
        dirty.run();
        return true;
    }

    /** 完成后直接通过 IE 的方法插入或掉落，不写入槽位。 */
    public void flushCompleted(Consumer<ItemStack> output) {
        if (remainingTicks > 0 || (batchInput.isEmpty() && pendingResults.isEmpty())) return;
        for (ItemStack result : pendingResults) emitStacks(result, output);
        pendingResults.clear();
        batchInput = ItemStack.EMPTY;
        dirty.run();
    }

    public int getRemainingTicks() { return remainingTicks; }

    public void dropStoredItems(Consumer<ItemStack> drop) {
        for (int i = 0; i < SLOTS; i++) emitStacks(inputs.extractItem(i, Integer.MAX_VALUE, false), drop);
        if (remainingTicks > 0) emitStacks(batchInput, drop);
        else for (ItemStack result : pendingResults) emitStacks(result, drop);
        batchInput = ItemStack.EMPTY;
        pendingResults.clear();
        remainingTicks = 0;
        dirty.run();
    }

    private static void emitStacks(ItemStack source, Consumer<ItemStack> drop) {
        ItemStack stack = source.copy();
        while (!stack.isEmpty()) drop.accept(stack.split(stack.getMaxStackSize()));
    }

    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("inputs", inputs.serializeNBT(provider));
        tag.put("energy", energy.serializeNBT(provider));
        tag.put("batch_input", batchInput.saveOptional(provider));
        tag.putInt("remaining_ticks", remainingTicks);
        ListTag results = new ListTag();
        for (ItemStack result : pendingResults) results.add(result.saveOptional(provider));
        tag.put("pending_results", results);
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        inputs.deserializeNBT(provider, tag.getCompound("inputs"));
        if (tag.contains("energy")) energy.deserializeNBT(provider, tag.get("energy"));
        energy.setStoredEnergy(Math.clamp(energy.getEnergyStored(), 0, ENERGY_CAPACITY));
        batchInput = ItemStack.parseOptional(provider, tag.getCompound("batch_input"));
        remainingTicks = Math.clamp(tag.getInt("remaining_ticks"), 0, PROCESS_TICKS);
        pendingResults.clear();
        for (Tag result : tag.getList("pending_results", Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parseOptional(provider, (CompoundTag)result);
            if (!stack.isEmpty()) pendingResults.add(stack);
        }
        if (batchInput.isEmpty()) remainingTicks = 0;
    }
}

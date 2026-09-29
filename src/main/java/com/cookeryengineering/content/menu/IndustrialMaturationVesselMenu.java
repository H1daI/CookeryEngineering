package com.cookeryengineering.content.menu;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import blusunrize.immersiveengineering.common.gui.sync.GenericContainerData;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import com.cookeryengineering.content.multiblock.IndustrialMaturationVesselLogic;
import com.cookeryengineering.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public final class IndustrialMaturationVesselMenu extends IEContainerMenu {
    public static final int MANUAL_START_BUTTON = 0;

    private static final int GUI_INPUT_SLOTS = 6;
    private static final int CONTAINER_SLOT = GUI_INPUT_SLOTS;
    private static final int FIRST_OUTPUT_SLOT = CONTAINER_SLOT + 1;
    private static final int MACHINE_SLOT_COUNT = FIRST_OUTPUT_SLOT
            + IndustrialMaturationVesselLogic.State.OUTPUT_SLOTS;
    private static final int DATA_COUNT = 3;

    private static final int ENERGY_DATA = 0;
    private static final int PROCESS_TICKS_DATA = 1;
    private static final int PROCESS_TOTAL_DATA = 2;

    private final ContainerData data;
    private final BooleanSupplier valid;
    private final BooleanSupplier manualStart;
    public final FluidTank tank;

    public static IndustrialMaturationVesselMenu makeClient(int id, Inventory playerInventory) {
        return new IndustrialMaturationVesselMenu(
                id,
                playerInventory,
                new ItemStackHandler(IndustrialMaturationVesselLogic.State.INPUT_SLOTS),
                new ItemStackHandler(1),
                new ItemStackHandler(IndustrialMaturationVesselLogic.State.OUTPUT_SLOTS),
                new SimpleContainerData(DATA_COUNT),
                new FluidTank(IndustrialMaturationVesselLogic.State.TANK_CAPACITY),
                () -> true,
                () -> false
        );
    }

    public static IndustrialMaturationVesselMenu makeServer(
            int id,
            Inventory playerInventory,
            IndustrialMaturationVesselLogic.State state,
            BooleanSupplier valid
    ) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case ENERGY_DATA -> state.getEnergy().getEnergyStored();
                    case PROCESS_TICKS_DATA -> state.getProcessTicks();
                    case PROCESS_TOTAL_DATA -> state.getProcessTicksTotal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 服务端负责机器状态
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        return new IndustrialMaturationVesselMenu(
                id,
                playerInventory,
                state.getInputs(),
                state.getContainer(),
                state.getOutputs(),
                data,
                state.getTank(),
                valid,
                () -> state.tryStartBatch(playerInventory.player.level())
        );
    }

    private IndustrialMaturationVesselMenu(
            int id,
            Inventory playerInventory,
            ItemStackHandler inputs,
            ItemStackHandler container,
            ItemStackHandler outputs,
            ContainerData data,
            FluidTank tank,
            BooleanSupplier valid,
            BooleanSupplier manualStart
    ) {
        super(clientCtx(ModMenuTypes.INDUSTRIAL_MATURATION_VESSEL.get(), id));
        this.tank = tank;
        addGenericData(GenericContainerData.fluid(tank));
        this.data = data;
        this.valid = valid;
        this.manualStart = manualStart;

        for (int slot = 0; slot < GUI_INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(inputs, slot, 69 + slot % 2 * 18, 32 + slot / 2 * 18) {
                @Override
                public int getMaxStackSize() { return 1; }

                @Override
                public int getMaxStackSize(ItemStack stack) { return 1; }
            });
        }
        addSlot(new SlotItemHandler(container, 0, 121, 86));
        for (int slot = 0; slot < IndustrialMaturationVesselLogic.State.OUTPUT_SLOTS; slot++) {
            addSlot(new OutputSlot(outputs, slot, 158 + slot % 4 * 18, 32 + slot / 4 * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        42 + column * 18, 125 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 42 + column * 18, 183));
        }
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != MANUAL_START_BUTTON) {
            return false;
        }
        if (!player.level().isClientSide && stillValid(player)) {
            manualStart.getAsBoolean();
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, MACHINE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, GUI_INPUT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return valid.getAsBoolean();
    }

    public int getEnergyStored() {
        return data.get(ENERGY_DATA);
    }

    public int getEnergyCapacity() {
        return IndustrialMaturationVesselLogic.State.ENERGY_CAPACITY;
    }

    public int getProcessTicks() {
        return data.get(PROCESS_TICKS_DATA);
    }

    public int getProcessTicksTotal() {
        return data.get(PROCESS_TOTAL_DATA);
    }

    public float getProgress() {
        int total = getProcessTicksTotal();
        return total <= 0 ? 0 : 1F - (float)getProcessTicks() / total;
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

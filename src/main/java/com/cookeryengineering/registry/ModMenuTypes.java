package com.cookeryengineering.registry;

import com.cookeryengineering.CookeryEngineering;
import com.cookeryengineering.content.menu.IndustrialMaturationVesselMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CookeryEngineering.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<IndustrialMaturationVesselMenu>>
            INDUSTRIAL_MATURATION_VESSEL = MENUS.register(
                    "industrial_maturation_vessel",
                    () -> new MenuType<>(IndustrialMaturationVesselMenu::makeClient, FeatureFlags.DEFAULT_FLAGS)
            );

    private ModMenuTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}

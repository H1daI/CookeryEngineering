package com.cookeryengineering;

import com.cookeryengineering.registry.ModItems;
import com.cookeryengineering.registry.ModMenuTypes;
import com.cookeryengineering.registry.ModMultiblocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CookeryEngineering.MOD_ID)
public final class CookeryEngineering {
    public static final String MOD_ID = "cookery_engineering";

    public CookeryEngineering(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModMultiblocks.register(modEventBus);
    }
}

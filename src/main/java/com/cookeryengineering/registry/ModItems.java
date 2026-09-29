package com.cookeryengineering.registry;

import com.cookeryengineering.CookeryEngineering;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CookeryEngineering.MOD_ID);

    public static final DeferredItem<Item> ROACH = ITEMS.registerSimpleItem("roach");
    public static final DeferredItem<Item> ARTHROPOD_BIOMASS =
            ITEMS.registerSimpleItem("arthropod_biomass");
    public static final DeferredItem<Item> CHITIN = ITEMS.registerSimpleItem("chitin");
    public static final DeferredItem<Item> BLATTS_PROTEIN_BAR = ITEMS.registerSimpleItem(
            "blatts_protein_bar",
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.8F)
                    .build())
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ROACH.get());
            event.accept(ARTHROPOD_BIOMASS.get());
            event.accept(CHITIN.get());
        } else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(BLATTS_PROTEIN_BAR.get());
        }
    }
}

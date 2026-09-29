package com.cookeryengineering.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree.InnerNode;
import com.cookeryengineering.CookeryEngineering;
import net.minecraft.resources.ResourceLocation;

/** 注册工程师手册 */
public final class CookeryEngineeringManual {
    private static final ResourceLocation CATEGORY = location("cookery_engineering");
    private static boolean registered;

    private CookeryEngineeringManual() {
    }

    public static void registerEntries() {
        if (registered) {
            return;
        }

        ManualInstance manual = ManualHelper.getManual();
        InnerNode<ResourceLocation, ManualEntry> category =
                manual.getRoot().getOrCreateSubnode(CATEGORY, 100);
        addEntry(manual, category, location("industrial_maturation_vessel"), 0);
        addEntry(manual, category, location("industrial_thresher"), 10);
        registered = true;
    }

    private static void addEntry(
            ManualInstance manual,
            InnerNode<ResourceLocation, ManualEntry> category,
            ResourceLocation location,
            int priority
    ) {
        ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(manual);
        builder.readFromFile(location);
        manual.addEntry(category, builder.create(), priority);
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(CookeryEngineering.MOD_ID, path);
    }
}

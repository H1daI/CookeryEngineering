package com.cookeryengineering.client;

import com.cookeryengineering.CookeryEngineering;
import com.cookeryengineering.client.gui.IndustrialMaturationVesselScreen;
import com.cookeryengineering.client.render.IndustrialMaturationVesselRenderer;
import com.cookeryengineering.registry.ModMenuTypes;
import com.cookeryengineering.registry.ModMultiblocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CookeryEngineering.MOD_ID, value = Dist.CLIENT)
public final class CookeryEngineeringClient {
    private CookeryEngineeringClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModMultiblocks.INDUSTRIAL_MATURATION_VESSEL.masterBE().get(),
                ignored -> new IndustrialMaturationVesselRenderer()
        );
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(
                ModMenuTypes.INDUSTRIAL_MATURATION_VESSEL.get(),
                IndustrialMaturationVesselScreen::new
        );
    }

    @SubscribeEvent
    public static void registerManualEntries(FMLLoadCompleteEvent event) {
        event.enqueueWork(CookeryEngineeringManual::registerEntries);
    }

}

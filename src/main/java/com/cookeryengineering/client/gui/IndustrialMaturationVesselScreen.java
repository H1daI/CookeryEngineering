package com.cookeryengineering.client.gui;

import blusunrize.immersiveengineering.client.gui.elements.GuiButtonIE;
import blusunrize.immersiveengineering.client.gui.info.FluidInfoArea;
import com.cookeryengineering.content.menu.IndustrialMaturationVesselMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


public final class IndustrialMaturationVesselScreen
        extends AbstractContainerScreen<IndustrialMaturationVesselMenu> {
    private static final ResourceLocation MIXER = ie("textures/gui/mixer.png");
    private static final ResourceLocation ARC = ie("textures/gui/arc_furnace.png");
    private static final ResourceLocation FERMENTER = ie("textures/gui/fermenter.png");
    private static final ResourceLocation ARROW = ie("textures/gui/alloy_smelter.png");
    private static final ResourceLocation BOWL = ResourceLocation.withDefaultNamespace("textures/item/bowl.png");
    private FluidInfoArea fluidArea;
    private GuiButtonIE startButton;

    public IndustrialMaturationVesselScreen(IndustrialMaturationVesselMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 244;
        imageHeight = 206;
    }

    private static ResourceLocation ie(String path) {
        return ResourceLocation.fromNamespaceAndPath("immersiveengineering", path);
    }

    @Override
    protected void init() {
        super.init();
        fluidArea = new FluidInfoArea(menu.tank, new Rect2i(leftPos + 36, topPos + 33, 16, 47),
                20, 51, ie("fermenter/tank_overlay"));
        startButton = addRenderableWidget(new GuiButtonIE(leftPos + 183, topPos + 88, 20, 20,
                Component.empty(), new GuiButtonIE.ButtonTexture(ie("circuit_table/register_button")),
                ignored -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                IndustrialMaturationVesselMenu.MANUAL_START_BUTTON);
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                int color = !active ? 0xff712622 : isHoveredOrFocused() ? 0xffff7060 : 0xffe33d32;
                int x = getX() + 5;
                int y = getY() + 4;
                graphics.fill(x + 4, y, x + 6, y + 6, color);
                graphics.fill(x + 1, y + 2, x + 3, y + 4, color);
                graphics.fill(x + 7, y + 2, x + 9, y + 4, color);
                graphics.fill(x, y + 4, x + 2, y + 8, color);
                graphics.fill(x + 8, y + 4, x + 10, y + 8, color);
                graphics.fill(x + 1, y + 8, x + 3, y + 10, color);
                graphics.fill(x + 7, y + 8, x + 9, y + 10, color);
                graphics.fill(x + 3, y + 9, x + 7, y + 11, color);
            }

            @Override
            protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
                return Component.translatable("gui.cookery_engineering.industrial_maturation_vessel.manual_start");
            }
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        startButton.active = menu.getProcessTicksTotal() == 0;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // IE 原有的UI元素
        for (int y = 4; y < 114; y += 16) {
            for (int x = 4; x < 240; x += 40) {
                patch(graphics, ARC, x, y, 10, 10, Math.min(40, 240 - x), Math.min(16, 114 - y));
            }
        }
        for (int x = 4; x < 240; x += 40) {
            patch(graphics, ARC, x, 0, 8, 0, Math.min(40, 240 - x), 4);
            patch(graphics, ARC, x, 114, 8, 114, Math.min(40, 240 - x), 4);
        }
        patch(graphics, ARC, 0, 0, 0, 0, 4, 118);
        patch(graphics, ARC, 240, 0, 172, 0, 4, 118);
        patch(graphics, MIXER, 34, 118, 0, 78, 176, 88);
        for (int i = 0; i < 6; i++) {
            slot(graphics, 69 + i % 2 * 18, 32 + i / 2 * 18);
        }
        for (int i = 0; i < 12; i++) {
            slot(graphics, 158 + i % 4 * 18, 32 + i / 4 * 18);
        }
        slot(graphics, 121, 86);
        // IE 的红色 IF 条和流体罐。
        patch(graphics, FERMENTER, 11, 32, 156, 20, 11, 50);
        int filled = Math.clamp(46 * menu.getEnergyStored() / menu.getEnergyCapacity(), 0, 46);
        graphics.fillGradient(leftPos + 13, topPos + 34 + 46 - filled,
                leftPos + 20, topPos + 80, 0xffb51500, 0xff600b00);
        patch(graphics, FERMENTER, 34, 31, 110, 19, 20, 51);
        fluidArea.draw(graphics);
        RenderSystem.enableBlend();
        graphics.setColor(0.3F, 0.3F, 0.3F, 1);
        patch(graphics, ARROW, 117, 49, 176, 14, 24, 17);
        graphics.setColor(1, 1, 1, 1);
        int progressWidth = Math.clamp(Math.round(24 * menu.getProgress()), 0, 24);
        if (progressWidth > 0) {
            patch(graphics, ARROW, 117, 49, 176, 14, progressWidth, 17);
        }
        if (!menu.getSlot(6).hasItem()) {
            RenderSystem.enableBlend();
            graphics.setColor(1, 1, 1, 0.35F);
            graphics.blit(BOWL, leftPos + 121, topPos + 86, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
        }
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 避免显示文字以保持简约
    }

    private void slot(GuiGraphics graphics, int x, int y) {
        patch(graphics, MIXER, x - 1, y - 1, 6, 5, 18, 18);
    }

    private void patch(GuiGraphics graphics, ResourceLocation texture,
                       int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture, leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }
}

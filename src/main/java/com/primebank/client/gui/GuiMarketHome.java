package com.primebank.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/*
 English: Minimal Market home placeholder. Lists and details will arrive in Phase 3.
 Español: Pantalla mínima de inicio del Mercado. Listados y detalles llegarán en la Fase 3.
*/
public class GuiMarketHome extends GuiScreen {
    private GuiButton btnClose;

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        btnClose = new GuiButton(0, midX - 40, midY + 20, 80, 20, I18n.format("ui.primebank.ok"));
        this.buttonList.add(btnClose);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.market.home.title");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnClose) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

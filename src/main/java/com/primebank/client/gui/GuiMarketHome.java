package com.primebank.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.Minecraft;

/*
 English: Minimal Market home placeholder. Lists and details will arrive in Phase 3.
 Español: Pantalla mínima de inicio del Mercado. Listados y detalles llegarán en la Fase 3.
*/
public class GuiMarketHome extends GuiScreen {
    private GuiButton btnMyCompany;
    private GuiButton btnOpenById;
    private GuiButton btnClose;
    private GuiTextField txtCompanyId;

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        int y = midY - 10;
        this.txtCompanyId = new GuiTextField(0, this.fontRenderer, midX - 100, y, 200, 20);
        this.txtCompanyId.setMaxStringLength(64);
        this.txtCompanyId.setText("");
        y += 28;
        btnMyCompany = new GuiButton(1, midX - 100, y, 98, 20, I18n.format("primebank.market.home.my_company"));
        btnOpenById = new GuiButton(2, midX + 2, y, 98, 20, I18n.format("primebank.market.home.open_id"));
        y += 24;
        btnClose = new GuiButton(3, midX - 40, y, 80, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnMyCompany);
        this.buttonList.add(btnOpenById);
        this.buttonList.add(btnClose);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.market.home.title");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        String hint = I18n.format("primebank.market.home.company_id_hint");
        drawCenteredString(this.fontRenderer, hint, this.width / 2, this.height / 2 - 24, 0xAAAAAA);
        this.txtCompanyId.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnMyCompany) {
            if (Minecraft.getMinecraft().player != null) {
                String cid = "c:" + Minecraft.getMinecraft().player.getUniqueID().toString();
                Minecraft.getMinecraft().displayGuiScreen(new GuiCompanyDetails(cid));
            }
        } else if (button == btnOpenById) {
            String cid = txtCompanyId.getText().trim();
            if (!cid.isEmpty()) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiCompanyDetails(cid));
            }
        } else if (button == btnClose) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}


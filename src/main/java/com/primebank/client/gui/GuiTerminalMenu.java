package com.primebank.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketTerminalOpenChargeRequest;

/*
 English: Minimal terminal menu to access Merchant Charge, Company Apply, and Market.
 Español: Menú mínimo del terminal para acceder a Cobro Comerciante, Solicitar Empresa y Mercado.
*/
public class GuiTerminalMenu extends GuiScreen {
    private GuiButton btnCharge;
    private GuiButton btnApply;
    private GuiButton btnMarket;

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        btnCharge = new GuiButton(0, midX - 100, midY - 20, 200, 20, I18n.format("ui.primebank.menu.charge"));
        btnApply = new GuiButton(1, midX - 100, midY + 4, 200, 20, I18n.format("ui.primebank.menu.apply_company"));
        btnMarket = new GuiButton(2, midX - 100, midY + 28, 200, 20, I18n.format("ui.primebank.menu.market"));
        this.buttonList.add(btnCharge);
        this.buttonList.add(btnApply);
        this.buttonList.add(btnMarket);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnCharge) {
            // English: Ask server to open company selection for terminal pricing.
            // Español: Pedir al servidor que abra la selección de empresa para el terminal.
            PrimeBankMod.NETWORK.sendToServer(new PacketTerminalOpenChargeRequest());
        } else if (button == btnApply) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiCompanyApply());
        } else if (button == btnMarket) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMarketHome());
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

package com.primebank.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketMarketBuy;

/*
 English: Dialog to enter number of shares to buy. Shows price and listed availability. Enforces basic constraints client-side.
 Español: Diálogo para ingresar número de acciones a comprar. Muestra precio y disponibilidad listada. Aplica restricciones básicas en el cliente.
*/
public class GuiBuyDialog extends GuiScreen {
    private final GuiCompanyDetails parent;
    private final String companyId;
    private final String displayName;
    private final long pricePerShareCents;
    private final int listedShares;

    private GuiTextField txtShares;
    private GuiButton btnConfirm;
    private GuiButton btnCancel;

    public GuiBuyDialog(GuiCompanyDetails parent, String companyId, String displayName, long pricePerShareCents, int listedShares) {
        this.parent = parent;
        this.companyId = companyId;
        this.displayName = displayName;
        this.pricePerShareCents = pricePerShareCents;
        this.listedShares = listedShares;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        this.txtShares = new GuiTextField(0, this.fontRenderer, midX - 60, midY, 120, 20);
        this.txtShares.setMaxStringLength(6);
        this.txtShares.setText("1");
        this.btnConfirm = new GuiButton(1, midX - 100, midY + 30, 98, 20, I18n.format("primebank.market.buydialog.buy"));
        this.btnCancel = new GuiButton(2, midX + 2, midY + 30, 98, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnConfirm);
        this.buttonList.add(btnCancel);
        updateButtonState();
    }

    private void updateButtonState() {
        int shares = parseShares();
        boolean valid = shares > 0 && shares <= Math.max(0, listedShares);
        btnConfirm.enabled = valid && pricePerShareCents > 0;
    }

    private int parseShares() {
        try {
            return Integer.parseInt(txtShares.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int midX = this.width / 2;
        int topY = this.height / 2 - 40;
        String title = I18n.format("primebank.market.buydialog.title", displayName);
        drawCenteredString(this.fontRenderer, title, midX, topY, 0xFFFFFF);
        topY += 14;
        String price = I18n.format("primebank.market.buydialog.price_each", com.primebank.core.Money.formatUsd(pricePerShareCents));
        drawCenteredString(this.fontRenderer, price, midX, topY, 0xDDDDDD);
        topY += 12;
        String listed = I18n.format("primebank.market.buydialog.listed_available", listedShares);
        drawCenteredString(this.fontRenderer, listed, midX, topY, 0xDDDDDD);
        topY += 18;
        String prompt = I18n.format("primebank.market.buydialog.enter_shares");
        drawCenteredString(this.fontRenderer, prompt, midX, topY, 0xDDDDDD);
        this.txtShares.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        this.txtShares.textboxKeyTyped(typedChar, keyCode);
        updateButtonState();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.txtShares.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnConfirm) {
            int shares = parseShares();
            if (shares > 0 && shares <= listedShares) {
                // English: Send buy request to server.
                // Español: Enviar solicitud de compra al servidor.
                PrimeBankMod.NETWORK.sendToServer(new PacketMarketBuy(companyId, shares));
            }
            // English: Return to details screen (it will refresh via details packet on success).
            // Español: Volver a la pantalla de detalles (se actualizará con el paquete de detalles si tiene éxito).
            Minecraft.getMinecraft().displayGuiScreen(parent);
        } else if (button == btnCancel) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

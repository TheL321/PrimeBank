package com.primebank.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;

import com.primebank.core.Money;
import com.primebank.PrimeBankMod;
import com.primebank.net.PacketSetPendingCharge;

/*
 English: Merchant GUI on the Terminal to enter an amount and press Charge.
 Español: GUI del comerciante en el Terminal para ingresar un monto y presionar Cobrar.
*/
public class GuiTerminalCharge extends GuiScreen {
    private final String companyId;
    private final String companyLabel;
    private GuiTextField input;
    private GuiButton btnCharge;
    private GuiButton btnCancel;

    public GuiTerminalCharge(String companyId, String companyLabel) {
        this.companyId = companyId;
        this.companyLabel = companyLabel;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        this.input = new GuiTextField(2, this.fontRenderer, midX - 100, midY - 10, 200, 20);
        this.input.setFocused(true);
        this.btnCharge = new GuiButton(0, midX - 100, midY + 20, 90, 20, I18n.format("ui.primebank.charge"));
        this.btnCancel = new GuiButton(1, midX + 10, midY + 20, 90, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnCharge);
        this.buttonList.add(btnCancel);
    }

    @Override
    public void updateScreen() {
        if (this.input != null) this.input.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.input != null && this.input.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.input != null) this.input.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.pos.terminal.title");
        String hint = I18n.format("primebank.pos.terminal.hint");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, hint, this.width / 2, this.height / 2 - 28, 0xAAAAAA);
        if (companyLabel != null && !companyLabel.isEmpty()) {
            // English: Show which company will receive the charges.
            // Español: Mostrar qué empresa recibirá los cobros.
            drawCenteredString(this.fontRenderer, companyLabel, this.width / 2, this.height / 2 - 52, 0xFFD700);
        }
        if (this.input != null) this.input.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnCharge) {
            String text = this.input.getText();
            try {
                long cents = Money.parseCents(text);
                PrimeBankMod.NETWORK.sendToServer(new PacketSetPendingCharge(cents, companyId));
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.pos.pending.set", Money.formatUsd(cents)));
                close();
            } catch (Exception ex) {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.pos.pending.bad_amount"));
            }
        } else if (button == btnCancel) {
            close();
        }
    }

    private void close() { Minecraft.getMinecraft().displayGuiScreen(null); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

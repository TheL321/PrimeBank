package com.primebank.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.primebank.core.Money;
import com.primebank.PrimeBankMod;
import com.primebank.net.PacketPosRespond;

/*
 English: Minimal POS confirmation GUI. Shows amount and Confirm/Cancel buttons.
 Español: GUI mínima de confirmación de POS. Muestra el monto y botones Confirmar/Cancelar.
*/
public class GuiPosPrompt extends GuiScreen {
    private final long cents;
    private GuiButton btnOk;
    private GuiButton btnCancel;

    public GuiPosPrompt(long cents) {
        this.cents = cents;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        this.btnOk = new GuiButton(0, midX - 100, midY + 10, 90, 20, I18n.format("ui.primebank.ok"));
        this.btnCancel = new GuiButton(1, midX + 10, midY + 10, 90, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnOk);
        this.buttonList.add(btnCancel);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.pos.prompt.title");
        String amount = I18n.format("primebank.pos.prompt.amount", Money.formatUsd(cents));
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, amount, this.width / 2, this.height / 2 - 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnOk) {
            PrimeBankMod.NETWORK.sendToServer(new PacketPosRespond(true, cents));
            close();
        } else if (button == btnCancel) {
            PrimeBankMod.NETWORK.sendToServer(new PacketPosRespond(false, cents));
            close();
        }
    }

    private void close() {
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

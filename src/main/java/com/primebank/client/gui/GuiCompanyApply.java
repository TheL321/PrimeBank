package com.primebank.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketCompanyApply;

/*
 English: Company application GUI. Player enters name and optional description.
 Español: GUI de solicitud de empresa. El jugador ingresa nombre y descripción opcional.
*/
public class GuiCompanyApply extends GuiScreen {
    private GuiTextField name;
    private GuiTextField desc;
    private GuiButton btnSubmit;
    private GuiButton btnCancel;

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        this.name = new GuiTextField(1, this.fontRenderer, midX - 100, midY - 30, 200, 20);
        this.name.setFocused(true);
        this.name.setMaxStringLength(64);
        this.desc = new GuiTextField(2, this.fontRenderer, midX - 100, midY, 200, 20);
        this.desc.setMaxStringLength(128);
        this.btnSubmit = new GuiButton(0, midX - 100, midY + 30, 90, 20, I18n.format("ui.primebank.ok"));
        this.btnCancel = new GuiButton(1, midX + 10, midY + 30, 90, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnSubmit);
        this.buttonList.add(btnCancel);
    }

    @Override
    public void updateScreen() {
        if (this.name != null) this.name.updateCursorCounter();
        if (this.desc != null) this.desc.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.name != null && this.name.textboxKeyTyped(typedChar, keyCode)) return;
        if (this.desc != null && this.desc.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.name != null) this.name.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.desc != null) this.desc.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.company.apply.title");
        String hint = I18n.format("primebank.company.apply.hint");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, hint, this.width / 2, this.height / 2 - 48, 0xAAAAAA);
        if (this.name != null) this.name.drawTextBox();
        if (this.desc != null) this.desc.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnSubmit) {
            String n = this.name.getText() == null ? "" : this.name.getText().trim();
            String d = this.desc.getText() == null ? "" : this.desc.getText().trim();
            if (n.isEmpty()) {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_name"));
                return;
            }
            PrimeBankMod.NETWORK.sendToServer(new PacketCompanyApply(n, d));
            close();
        } else if (button == btnCancel) {
            close();
        }
    }

    private void close() { Minecraft.getMinecraft().displayGuiScreen(null); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

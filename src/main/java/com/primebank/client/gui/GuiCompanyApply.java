package com.primebank.client.gui;

import java.io.IOException;
import java.util.Locale;

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
    private GuiTextField shortName;
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
        this.shortName = new GuiTextField(3, this.fontRenderer, midX - 100, midY + 30, 200, 18);
        this.shortName.setMaxStringLength(12);
        this.shortName.setText("");
        this.btnSubmit = new GuiButton(0, midX - 100, midY + 60, 90, 20, I18n.format("ui.primebank.ok"));
        this.btnCancel = new GuiButton(1, midX + 10, midY + 60, 90, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnSubmit);
        this.buttonList.add(btnCancel);
    }

    @Override
    public void updateScreen() {
        if (this.name != null) this.name.updateCursorCounter();
        if (this.desc != null) this.desc.updateCursorCounter();
        if (this.shortName != null) this.shortName.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.name != null && this.name.textboxKeyTyped(typedChar, keyCode)) return;
        if (this.desc != null && this.desc.textboxKeyTyped(typedChar, keyCode)) return;
        if (this.shortName != null && this.shortName.textboxKeyTyped(typedChar, keyCode)) {
            String sanitized = sanitizeTicker(this.shortName.getText());
            this.shortName.setText(sanitized);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.name != null) this.name.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.desc != null) this.desc.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.shortName != null) this.shortName.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.company.apply.title");
        String hint = I18n.format("primebank.company.apply.hint");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, hint, this.width / 2, this.height / 2 - 48, 0xAAAAAA);
        // English: Field labels and hints for Name, Description, and Ticker.
        // Español: Etiquetas y pistas de los campos para Nombre, Descripción y Ticker.
        String nameLabel = I18n.format("primebank.company.apply.name_label");
        drawCenteredString(this.fontRenderer, nameLabel, this.width / 2, this.height / 2 - 44, 0xFFFFFF);
        String nameHint = I18n.format("primebank.company.apply.name_hint");
        drawCenteredString(this.fontRenderer, nameHint, this.width / 2, this.height / 2 - 34, 0xAAAAAA);

        String descLabel = I18n.format("primebank.company.apply.desc_label");
        drawCenteredString(this.fontRenderer, descLabel, this.width / 2, this.height / 2 - 12, 0xFFFFFF);
        String descHint = I18n.format("primebank.company.apply.desc_hint");
        drawCenteredString(this.fontRenderer, descHint, this.width / 2, this.height / 2 - 2, 0xAAAAAA);
        String shortLabel = I18n.format("primebank.company.apply.short_label");
        drawCenteredString(this.fontRenderer, shortLabel, this.width / 2, this.height / 2 + 12, 0xFFFFFF);
        String shortHint = I18n.format("primebank.company.apply.short_hint");
        drawCenteredString(this.fontRenderer, shortHint, this.width / 2, this.height / 2 + 22, 0xAAAAAA);
        if (this.name != null) this.name.drawTextBox();
        if (this.desc != null) this.desc.drawTextBox();
        if (this.shortName != null) this.shortName.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnSubmit) {
            String n = this.name.getText() == null ? "" : this.name.getText().trim();
            String d = this.desc.getText() == null ? "" : this.desc.getText().trim();
            String ticker = sanitizeTicker(this.shortName.getText());
            if (n.isEmpty()) {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_name"));
                return;
            }
            if (ticker.length() < 2 || ticker.length() > 8) {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_short"));
                return;
            }
            PrimeBankMod.NETWORK.sendToServer(new PacketCompanyApply(n, d, ticker));
            close();
        } else if (button == btnCancel) {
            close();
        }
    }

    private void close() { Minecraft.getMinecraft().displayGuiScreen(null); }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private String sanitizeTicker(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replaceAll("[^A-Za-z0-9]", "");
        return cleaned.toUpperCase(Locale.ROOT);
    }
}

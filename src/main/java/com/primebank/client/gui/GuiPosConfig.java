package com.primebank.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import com.primebank.core.Money;
import com.primebank.PrimeBankMod;
import com.primebank.net.PacketPosSetPrice;

/*
 English: Owner-only GUI on the POS to set a specific price for that POS.
 Español: GUI solo para el dueño en el POS para establecer un precio específico para ese POS.
*/
public class GuiPosConfig extends GuiScreen {
    private final BlockPos pos;
    private final long currentCents;
    private GuiTextField input;
    private GuiButton btnSave;
    private GuiButton btnCancel;

    public GuiPosConfig(BlockPos pos, long currentCents) {
        this.pos = pos;
        this.currentCents = currentCents;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        this.input = new GuiTextField(2, this.fontRenderer, midX - 100, midY - 10, 200, 20);
        this.input.setFocused(true);
        if (currentCents > 0) this.input.setText(Money.formatUsd(currentCents));
        this.btnSave = new GuiButton(0, midX - 100, midY + 20, 90, 20, I18n.format("ui.primebank.ok"));
        this.btnCancel = new GuiButton(1, midX + 10, midY + 20, 90, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnSave);
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
        String title = I18n.format("primebank.pos.config.title");
        String hint = I18n.format("primebank.pos.config.hint");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, hint, this.width / 2, this.height / 2 - 28, 0xAAAAAA);
        if (this.input != null) this.input.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnSave) {
            String text = this.input.getText();
            try {
                long cents = Money.parseCents(text);
                PrimeBankMod.NETWORK.sendToServer(new PacketPosSetPrice(pos, cents));
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.pos.price.set", Money.formatUsd(cents)));
                close();
            } catch (Exception ex) {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("primebank.pos.price.bad_amount"));
            }
        } else if (button == btnCancel) {
            close();
        }
    }

    private void close() { Minecraft.getMinecraft().displayGuiScreen(null); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

package com.primebank.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketPosSetCompany;

/*
 English: GUI to select which company to assign a POS to. Shows a paginated list of the player's companies.
 Español: GUI para seleccionar a qué empresa asignar un POS. Muestra una lista paginada de las empresas del jugador.
*/
public class GuiPosSelectCompany extends GuiScreen {
    private final BlockPos pos;
    private final List<String> ids;
    private final List<String> labels;

    private GuiButton btnPrev;
    private GuiButton btnNext;
    private GuiButton btnCancel;

    private int page = 0;
    private static final int PAGE_SIZE = 8;

    public GuiPosSelectCompany(BlockPos pos, List<String> ids, List<String> labels) {
        this.pos = pos;
        this.ids = ids;
        this.labels = labels;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int midX = this.width / 2;
        int midY = this.height / 2;
        int startY = midY - 40;
        int visible = Math.min(PAGE_SIZE, labels.size() - page * PAGE_SIZE);
        for (int i = 0; i < visible; i++) {
            int idx = page * PAGE_SIZE + i;
            // English: One button per company label.
            // Español: Un botón por etiqueta de empresa.
            this.buttonList.add(new GuiButton(100 + i, midX - 120, startY + i * 22, 240, 20, labels.get(idx)));
        }
        // English: Pagination and cancel controls.
        // Español: Controles de paginación y cancelar.
        this.btnPrev = new GuiButton(1, midX - 120, midY + 110, 60, 20, I18n.format("ui.primebank.prev"));
        this.btnNext = new GuiButton(2, midX - 60, midY + 110, 60, 20, I18n.format("ui.primebank.next"));
        this.btnCancel = new GuiButton(0, midX + 10, midY + 110, 110, 20, I18n.format("ui.primebank.cancel"));
        this.btnPrev.enabled = page > 0;
        int maxPage = (labels.size() - 1) / PAGE_SIZE;
        this.btnNext.enabled = page < maxPage;
        this.buttonList.add(btnPrev);
        this.buttonList.add(btnNext);
        this.buttonList.add(btnCancel);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.pos.select_company.title");
        String hint = I18n.format("primebank.pos.select_company.hint");
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, hint, this.width / 2, this.height / 2 - 68, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnCancel) {
            close();
            return;
        }
        if (button == btnPrev) {
            if (page > 0) {
                page--;
                initGui();
            }
            return;
        }
        if (button == btnNext) {
            int maxPage = (labels.size() - 1) / PAGE_SIZE;
            if (page < maxPage) {
                page++;
                initGui();
            }
            return;
        }
        if (button.id >= 100) {
            int idxInPage = button.id - 100;
            int idx = page * PAGE_SIZE + idxInPage;
            if (idx >= 0 && idx < ids.size()) {
                String companyId = ids.get(idx);
                // English: Send selection to server and close.
                // Español: Enviar selección al servidor y cerrar.
                PrimeBankMod.NETWORK.sendToServer(new PacketPosSetCompany(pos, companyId));
                close();
            }
        }
    }

    private void close() { Minecraft.getMinecraft().displayGuiScreen(null); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

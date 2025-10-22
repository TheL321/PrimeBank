package com.primebank.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

/*
 English: Market home screen showing approved companies with valuation summaries.
 Español: Pantalla de inicio del mercado que muestra empresas aprobadas con resúmenes de valoración.
*/
public class GuiMarketHome extends GuiScreen {
    private GuiButton btnMyCompany;
    private GuiButton btnOpenById;
    private GuiButton btnClose;
    private GuiButton btnRefresh;
    private GuiTextField txtCompanyId;
    private List<CompanyEntry> entries = Collections.emptyList();
    private String statusLine = "";
    private int listTop;
    private int listBottom;
    private int listLeft;
    private int listRight;
    private float scrollOffset = 0f;
    private static final int ROW_HEIGHT = 38;

    @Override
    public void initGui() {
        int midX = this.width / 2;
        this.buttonList.clear();
        int top = 40;
        this.txtCompanyId = new GuiTextField(0, this.fontRenderer, midX - 100, top, 200, 20);
        this.txtCompanyId.setMaxStringLength(64);
        this.txtCompanyId.setText("");

        btnMyCompany = new GuiButton(1, midX - 154, top + 24, 120, 20, I18n.format("primebank.market.home.my_company"));
        btnOpenById = new GuiButton(2, midX - 30, top + 24, 120, 20, I18n.format("primebank.market.home.open_id"));
        btnRefresh = new GuiButton(4, midX + 94, top + 24, 120, 20, I18n.format("primebank.market.home.refresh"));
        btnClose = new GuiButton(3, midX - 40, this.height - 28, 80, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnMyCompany);
        this.buttonList.add(btnOpenById);
        this.buttonList.add(btnRefresh);
        this.buttonList.add(btnClose);

        listTop = top + 56;
        listBottom = this.height - 48;
        if (listBottom <= listTop) listBottom = listTop + 40;
        listLeft = 32;
        listRight = this.width - 32;
        scrollOffset = 0f;
        reloadEntries();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.market.home.title");
        drawCenteredString(this.fontRenderer, title, this.width / 2, 16, 0xFFFFFF);
        String hint = I18n.format("primebank.market.home.company_id_hint");
        drawCenteredString(this.fontRenderer, hint, this.width / 2, 28, 0xAAAAAA);
        this.txtCompanyId.drawTextBox();
        drawEntries(mouseX, mouseY);
        if (statusLine != null && !statusLine.isEmpty()) {
            drawCenteredString(this.fontRenderer, statusLine, this.width / 2, this.height - 42, 0x66CCFF);
        }
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
        } else if (button == btnRefresh) {
            reloadEntries();
        } else if (button == btnClose) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (this.txtCompanyId.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.txtCompanyId.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && mouseY >= listTop && mouseY <= listBottom && mouseX >= listLeft && mouseX <= listRight) {
            int relY = mouseY - listTop;
            int index = (int)((relY + scrollOffset) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                CompanyEntry entry = entries.get(index);
                Minecraft.getMinecraft().displayGuiScreen(new GuiCompanyDetails(entry.companyId));
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    /*
     English: Refresh entries from PrimeBankState to show approved companies.
     Español: Refrescar entradas desde PrimeBankState para mostrar empresas aprobadas.
    */
    private void reloadEntries() {
        com.primebank.core.state.PrimeBankState state = com.primebank.core.state.PrimeBankState.get();
        Map<String, String> names = state.getAllCompanyNames();
        Map<String, String> shortNames = state.getAllCompanyShortNames();
        List<CompanyEntry> listEntries = new ArrayList<>();
        for (com.primebank.core.company.Company company : com.primebank.core.state.PrimeBankState.get().companies().all()) {
            if (company == null || !company.approved) continue;
            String displayName = names.getOrDefault(company.id, company.id);
            String ticker = shortNames.getOrDefault(company.id, "");
            if (ticker != null && !ticker.trim().isEmpty()) {
                // English: Append ticker in parentheses for clarity. Español: Añadir el ticker entre paréntesis para mayor claridad.
                displayName = String.format("%s (%s)", displayName, ticker.trim());
            }
            listEntries.add(new CompanyEntry(displayName, ticker, company));
        }
        listEntries.sort(Comparator.comparing(e -> e.displayName.toLowerCase()));
        this.entries = listEntries;
        this.scrollOffset = 0f;
        statusLine = entries.isEmpty() ? I18n.format("primebank.market.home.no_companies") : I18n.format("primebank.market.home.total_companies", entries.size());
    }

    /*
     English: Entry representing a company with valuation and listed share info.
     Español: Entrada que representa una empresa con información de valoración y acciones listadas.
    */
    private static class CompanyEntry {
        final String displayName;
        final String companyId;
        final long valuation;
        final long pricePerShare;
        final int listedShares;
        final boolean blocked;
        final String shortName;

        CompanyEntry(String displayName, String shortName, com.primebank.core.company.Company company) {
            this.displayName = displayName;
            this.shortName = shortName == null ? "" : shortName;
            this.companyId = company.id;
            this.valuation = company.valuationCurrentCents;
            this.pricePerShare = company.valuationCurrentCents <= 0 ? 0 : (company.valuationCurrentCents / 101L);
            this.listedShares = company.listedShares;
            this.blocked = company.valuationCurrentCents <= 0;
        }
    }

    /*
     English: Render the company list with scrolling support.
     Español: Renderizar la lista de empresas con soporte de desplazamiento.
    */
    private void drawEntries(int mouseX, int mouseY) {
        if (entries.isEmpty()) return;
        boolean hoverList = mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom;
        handleScrollWheel(hoverList);
        int availableHeight = listBottom - listTop;
        int maxIndex = entries.size() - 1;
        float maxScroll = Math.max(0, maxIndex * ROW_HEIGHT - availableHeight + ROW_HEIGHT);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int startIndex = (int)(scrollOffset / ROW_HEIGHT);
        float offsetWithin = scrollOffset % ROW_HEIGHT;
        int y = listTop - (int)offsetWithin;

        for (int idx = startIndex; idx < entries.size(); idx++) {
            if (y > listBottom) break;
            CompanyEntry entry = entries.get(idx);
            int rowTop = y;
            int rowBottom = y + ROW_HEIGHT - 4;
            drawRect(listLeft, rowTop, listRight, rowBottom, 0x44000000);
            int nameColor = entry.blocked ? 0xFF6666 : 0xFFFFFF;
            this.fontRenderer.drawString(entry.displayName, listLeft + 6, rowTop + 4, nameColor);
            String valuationStr = I18n.format("primebank.market.listing.valuation", com.primebank.core.Money.formatUsd(entry.valuation));
            String priceStr = I18n.format("primebank.market.listing.price", com.primebank.core.Money.formatUsd(entry.pricePerShare));
            String listedStr = I18n.format("primebank.market.listing.listed", entry.listedShares);
            this.fontRenderer.drawString(TextFormatting.GRAY + valuationStr, listLeft + 10, rowTop + 16, 0xAAAAAA);
            this.fontRenderer.drawString(TextFormatting.GRAY + priceStr, listLeft + 10, rowTop + 26, 0xAAAAAA);
            this.fontRenderer.drawString(TextFormatting.GRAY + listedStr, listRight - 110, rowTop + 16, 0xAAAAAA);
            if (entry.blocked) {
                String blockedMsg = I18n.format("primebank.market.details.blocked");
                this.fontRenderer.drawString(TextFormatting.RED + blockedMsg, listRight - 110, rowTop + 26, 0xFF7777);
            }
            y += ROW_HEIGHT;
        }
    }

    /*
     English: Adjust scroll offset when the mouse wheel moves over the list.
     Español: Ajustar el desplazamiento cuando la rueda del ratón se mueve sobre la lista.
    */
    private void handleScrollWheel(boolean hoverList) {
        if (!hoverList) return;
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float step = ROW_HEIGHT * 0.6f;
            scrollOffset -= Math.signum(wheel) * step;
        }
    }
}


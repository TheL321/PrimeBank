package com.primebank.client.gui;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketMarketDetailsRequest;

/*
 English: Company details screen for the Market. Requests details from server and shows price, listed shares, and player's holdings. Allows buying when trading is enabled.
 Español: Pantalla de detalles de la empresa en el Mercado. Solicita detalles al servidor y muestra precio, acciones listadas y tenencias del jugador. Permite comprar cuando el comercio está habilitado.
*/
public class GuiCompanyDetails extends GuiScreen {
    private final String companyId;

    // Data populated by S2C packet
    private String displayName = null;
    private long valuationCurrentCents = 0L;
    private long[] valuationHistory = new long[0];
    private long pricePerShareCents = 0L;
    private int listedShares = 0;
    private int yourHoldings = 0;
    private boolean tradingBlocked = true;
    private boolean youAreOwner = false;

    private GuiButton btnBuy;
    private GuiButton btnRefresh;
    private GuiButton btnClose;

    public GuiCompanyDetails(String companyId) {
        this.companyId = companyId;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int baseY = this.height / 2 + 20;
        this.buttonList.clear();
        btnBuy = new GuiButton(0, midX - 100, baseY, 200, 20, I18n.format("primebank.market.details.buy"));
        btnRefresh = new GuiButton(1, midX - 100, baseY + 24, 98, 20, I18n.format("primebank.market.details.refresh"));
        btnClose = new GuiButton(2, midX + 2, baseY + 24, 98, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnBuy);
        this.buttonList.add(btnRefresh);
        this.buttonList.add(btnClose);
        requestDetails();
        updateButtons();
    }

    private void requestDetails() {
        // English: Ask the server for latest details.
        // Español: Pedir al servidor los últimos detalles.
        PrimeBankMod.NETWORK.sendToServer(new PacketMarketDetailsRequest(companyId));
    }

    private void updateButtons() {
        boolean canBuy = !tradingBlocked && pricePerShareCents > 0 && listedShares > 0;
        btnBuy.enabled = canBuy;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnBuy) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiBuyDialog(this, companyId, displayNameOrId(), pricePerShareCents, listedShares));
        } else if (button == btnRefresh) {
            requestDetails();
        } else if (button == btnClose) {
            this.mc.displayGuiScreen(null);
        }
    }

    private String displayNameOrId() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : companyId;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = I18n.format("primebank.market.details.title", displayNameOrId());
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);

        int y = this.height / 2 - 30;
        String valuationLine = I18n.format("primebank.market.details.valuation", com.primebank.core.Money.formatUsd(valuationCurrentCents));
        this.drawCenteredString(this.fontRenderer, valuationLine, this.width / 2, y, 0xDDDDDD); y += 12;
        if (valuationHistory.length > 0) {
            String historyLine = I18n.format("primebank.market.details.valuation_history", valuationHistory.length, formatValuationHistory());
            this.drawCenteredString(this.fontRenderer, historyLine, this.width / 2, y, 0xBBBBBB); y += 12;
        }
        String priceLine = I18n.format("primebank.market.details.price", com.primebank.core.Money.formatUsd(pricePerShareCents));
        this.drawCenteredString(this.fontRenderer, priceLine, this.width / 2, y, 0xDDDDDD); y += 12;
        String listedLine = I18n.format("primebank.market.details.listed", listedShares);
        this.drawCenteredString(this.fontRenderer, listedLine, this.width / 2, y, 0xDDDDDD); y += 12;
        String yourLine = I18n.format("primebank.market.details.your_holdings", yourHoldings);
        this.drawCenteredString(this.fontRenderer, yourLine, this.width / 2, y, 0xDDDDDD); y += 12;
        if (youAreOwner) {
            String ownerLine = I18n.format("primebank.market.details.you_are_owner");
            this.drawCenteredString(this.fontRenderer, ownerLine, this.width / 2, y, 0x99FF99); y += 12;
        }
        if (tradingBlocked) {
            String blocked = I18n.format("primebank.market.details.blocked");
            this.drawCenteredString(this.fontRenderer, blocked, this.width / 2, y, 0xFF6666); y += 12;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    /*
     English: Called by the S2C packet handler to update this screen with latest details.
     Español: Llamado por el manejador S2C para actualizar esta pantalla con los últimos detalles.
    */
    public void onDetails(String displayName, long valuationCurrentCents, long[] valuationHistory, long pricePerShareCents, int listedShares, int yourHoldings, boolean tradingBlocked, boolean youAreOwner) {
        this.displayName = displayName;
        this.valuationCurrentCents = valuationCurrentCents;
        this.valuationHistory = valuationHistory == null ? new long[0] : Arrays.copyOf(valuationHistory, valuationHistory.length);
        this.pricePerShareCents = pricePerShareCents;
        this.listedShares = listedShares;
        this.yourHoldings = yourHoldings;
        this.tradingBlocked = tradingBlocked;
        this.youAreOwner = youAreOwner;
        updateButtons();
    }

    /*
     English: Build a concise valuation history string from the latest entries.
     Español: Construir una cadena concisa del historial de valoraciones con las entradas más recientes.
    */
    private String formatValuationHistory() {
        if (valuationHistory == null || valuationHistory.length == 0) return "-";
        int max = Math.min(5, valuationHistory.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append(" | ");
            long val = valuationHistory[valuationHistory.length - 1 - i];
            sb.append(com.primebank.core.Money.formatUsd(val));
        }
        return sb.toString();
    }
}

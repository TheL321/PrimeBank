package com.primebank.client.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;

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
        int centerX = this.width / 2;
        int y = this.height / 2 - 60;
        int maxWidth = Math.max(160, this.width - 80);
        drawCenteredScaledString(title, centerX, y, 0xFFFFFF, maxWidth);

        y = this.height / 2 - 30;
        String valuationLine = I18n.format("primebank.market.details.valuation", com.primebank.core.Money.formatUsd(valuationCurrentCents));
        drawCenteredScaledString(valuationLine, centerX, y, 0xDDDDDD, maxWidth); y += 12;
        if (valuationHistory.length > 0) {
            String historyLine = I18n.format("primebank.market.details.valuation_history", valuationHistory.length, formatValuationHistory());
            y = drawWrappedCentered(historyLine, centerX, y, 0xBBBBBB, maxWidth);
        }
        String priceLine = I18n.format("primebank.market.details.price", com.primebank.core.Money.formatUsd(pricePerShareCents));
        drawCenteredScaledString(priceLine, centerX, y, 0xDDDDDD, maxWidth); y += 12;
        String listedLine = I18n.format("primebank.market.details.listed", listedShares);
        drawCenteredScaledString(listedLine, centerX, y, 0xDDDDDD, maxWidth); y += 12;
        String yourLine = I18n.format("primebank.market.details.your_holdings", yourHoldings);
        drawCenteredScaledString(yourLine, centerX, y, 0xDDDDDD, maxWidth); y += 12;
        if (youAreOwner) {
            String ownerLine = I18n.format("primebank.market.details.you_are_owner");
            drawCenteredScaledString(ownerLine, centerX, y, 0x99FF99, maxWidth); y += 12;
        }
        if (tradingBlocked) {
            String blocked = I18n.format("primebank.market.details.blocked");
            drawCenteredScaledString(blocked, centerX, y, 0xFF6666, maxWidth); y += 12;
        }
        adjustButtonPositions(y);
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

    /*
     English: Move buttons down if content extends near them to avoid overlap.
     Español: Mover los botones hacia abajo si el contenido se acerca para evitar superposición.
    */
    private void adjustButtonPositions(int contentBottomY) {
        if (btnBuy == null || btnRefresh == null || btnClose == null) return;
        int defaultTop = this.height / 2 + 20;
        int safeTop = Math.max(defaultTop, contentBottomY + 12);
        btnBuy.y = safeTop;
        btnRefresh.y = safeTop + 24;
        btnClose.y = safeTop + 24;
        btnRefresh.x = this.width / 2 - 100;
        btnClose.x = this.width / 2 + 2;
    }

    /*
     English: Draw centered text with optional scaling to fit within maxWidth.
     Español: Dibujar texto centrado con escala opcional para ajustarse a maxWidth.
    */
    private void drawCenteredScaledString(String text, int centerX, int y, int color, int maxWidth) {
        if (text == null) return;
        int width = this.fontRenderer.getStringWidth(text);
        if (width <= 0) return;
        float scale = 1.0F;
        if (width > maxWidth) {
            scale = (float) maxWidth / (float) width;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        this.fontRenderer.drawString(text, -this.fontRenderer.getStringWidth(text) / 2, 0, color, false);
        GlStateManager.popMatrix();
    }

    /*
     English: Wrap long text into multiple centered lines and return updated Y position.
     Español: Dividir texto largo en varias líneas centradas y devolver la posición Y actualizada.
    */
    private int drawWrappedCentered(String text, int centerX, int startY, int color, int maxWidth) {
        if (text == null || text.isEmpty()) return startY;
        int y = startY;
        List<String> lines = this.fontRenderer.listFormattedStringToWidth(text, Math.max(1, maxWidth));
        for (String line : lines) {
            drawCenteredScaledString(line, centerX, y, color, maxWidth);
            y += 12;
        }
        return y;
    }
}

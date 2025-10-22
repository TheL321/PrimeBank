package com.primebank.client.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;

import org.lwjgl.input.Mouse;

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
    private String shortName = null;
    private long valuationCurrentCents = 0L;
    private long[] valuationHistory = new long[0];
    private long pricePerShareCents = 0L;
    private int listedShares = 0;
    private int yourHoldings = 0;
    private boolean tradingBlocked = true;
    private boolean youAreOwner = false;
    private long detailsFetchedAtMs = 0L;
    private int contentTop;
    private int contentBottom;
    private int contentLeft;
    private int contentRight;
    private float scrollOffset = 0f;

    private GuiButton btnBuy;
    private GuiButton btnRefresh;
    private GuiButton btnClose;

    public GuiCompanyDetails(String companyId) {
        this.companyId = companyId;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int baseY = this.height - 52; // English: Fix buttons near the bottom. Español: Fijar botones cerca del fondo.
        this.buttonList.clear();
        btnBuy = new GuiButton(0, midX - 100, baseY, 200, 20, I18n.format("primebank.market.details.buy"));
        btnRefresh = new GuiButton(1, midX - 100, baseY + 24, 98, 20, I18n.format("primebank.market.details.refresh"));
        btnClose = new GuiButton(2, midX + 2, baseY + 24, 98, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnBuy);
        this.buttonList.add(btnRefresh);
        this.buttonList.add(btnClose);
        // English: Define scrollable content viewport. Español: Definir el área de contenido desplazable.
        contentTop = 28;
        contentBottom = baseY - 12;
        if (contentBottom <= contentTop) contentBottom = contentTop + 40;
        contentLeft = 20;
        contentRight = this.width - 20;
        scrollOffset = 0f;
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
        String base = (displayName != null && !displayName.isEmpty()) ? displayName : companyId;
        if (shortName != null && !shortName.isEmpty()) {
            return String.format("%s (%s)", base, shortName);
        }
        return base;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        // English: Draw viewport background. Español: Dibujar fondo del área de vista.
        Gui.drawRect(contentLeft, contentTop, contentRight, contentBottom, 0x22000000);
        drawContent(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /*
     English: Render content inside a scrollable viewport.
     Español: Renderizar contenido dentro de una ventana desplazable.
    */
    private void drawContent(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int maxWidth = Math.max(160, this.width - 80);
        boolean hover = mouseX >= contentLeft && mouseX <= contentRight && mouseY >= contentTop && mouseY <= contentBottom;
        handleScrollWheel(hover);
        int visibleH = contentBottom - contentTop;
        int totalH = computeContentHeight();
        if (totalH < visibleH) totalH = visibleH;
        if (scrollOffset < 0f) scrollOffset = 0f;
        float maxScroll = Math.max(0f, totalH - visibleH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int y = contentTop + 4 - (int)scrollOffset;
        // Title
        if (y + 12 >= contentTop && y <= contentBottom)
            drawCenteredScaledString(I18n.format("primebank.market.details.title", displayNameOrId()), centerX, y, 0xFFFFFF, maxWidth);
        y += 14;
        // Valuation
        if (y + 12 >= contentTop && y <= contentBottom)
            drawCenteredScaledString(I18n.format("primebank.market.details.valuation", com.primebank.core.Money.formatUsd(valuationCurrentCents)), centerX, y, 0xDDDDDD, maxWidth);
        y += 12;
        // Graph and summary
        if (valuationHistory.length > 0) {
            int graphBlock = 70 + 12; // graph height + caption
            if (y + graphBlock >= contentTop && y <= contentBottom) {
                y = drawGraph(centerX, y);
            } else {
                y += graphBlock;
            }
            y += 6; // spacing
            if (valuationHistory.length >= 2) {
                if (y + 12 >= contentTop && y <= contentBottom)
                    drawHistorySummary(centerX, y, maxWidth);
                y += 16;
            }
        }
        // Price
        if (y + 12 >= contentTop && y <= contentBottom)
            drawCenteredScaledString(I18n.format("primebank.market.details.price", com.primebank.core.Money.formatUsd(pricePerShareCents)), centerX, y, 0xDDDDDD, maxWidth);
        y += 12;
        // Listed
        if (y + 12 >= contentTop && y <= contentBottom)
            drawCenteredScaledString(I18n.format("primebank.market.details.listed", listedShares), centerX, y, 0xDDDDDD, maxWidth);
        y += 12;
        // Holdings
        if (y + 12 >= contentTop && y <= contentBottom)
            drawCenteredScaledString(I18n.format("primebank.market.details.your_holdings", yourHoldings), centerX, y, 0xDDDDDD, maxWidth);
        y += 12;
        // Owner note
        if (youAreOwner) {
            if (y + 12 >= contentTop && y <= contentBottom)
                drawCenteredScaledString(I18n.format("primebank.market.details.you_are_owner"), centerX, y, 0x99FF99, maxWidth);
            y += 12;
        }
        // Trading blocked note
        if (tradingBlocked) {
            if (y + 12 >= contentTop && y <= contentBottom)
                drawCenteredScaledString(I18n.format("primebank.market.details.blocked"), centerX, y, 0xFF6666, maxWidth);
            y += 12;
        }
    }

    /*
     English: Calculate total content height for scrolling.
     Español: Calcular la altura total del contenido para el desplazamiento.
    */
    private int computeContentHeight() {
        int h = 0;
        h += 14; // title
        h += 12; // valuation
        if (valuationHistory.length > 0) {
            h += 70 + 12; // graph + caption
            h += 6; // spacing
            if (valuationHistory.length >= 2) {
                h += 16; // summary + spacing
            }
        }
        h += 12; // price
        h += 12; // listed
        h += 12; // holdings
        if (youAreOwner) h += 12;
        if (tradingBlocked) h += 12;
        return h + 4; // small padding
    }

    /*
     English: Handle mouse wheel to adjust scroll offset.
     Español: Manejar la rueda del ratón para ajustar el desplazamiento.
    */
    private void handleScrollWheel(boolean hover) {
        if (!hover) return;
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            scrollOffset -= (wheel / 120f) * 24f; // English: step per wheel notch. Español: paso por muesca.
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    /*
     English: Called by the S2C packet handler to update this screen with latest details.
     Español: Llamado por el manejador S2C para actualizar esta pantalla con los últimos detalles.
    */
    public void onDetails(String displayName, String shortName, long valuationCurrentCents, long[] valuationHistory, long pricePerShareCents, int listedShares, int yourHoldings, boolean tradingBlocked, boolean youAreOwner) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.valuationCurrentCents = valuationCurrentCents;
        this.valuationHistory = valuationHistory == null ? new long[0] : Arrays.copyOf(valuationHistory, valuationHistory.length);
        this.pricePerShareCents = pricePerShareCents;
        this.listedShares = listedShares;
        this.yourHoldings = yourHoldings;
        this.tradingBlocked = tradingBlocked;
        this.youAreOwner = youAreOwner;
        this.detailsFetchedAtMs = System.currentTimeMillis();
        updateButtons();
    }

    /*
     English: Draw valuation history as a simple filled graph for the last 26 weeks.
     Español: Dibujar el historial de valoración como un gráfico relleno para las últimas 26 semanas.
    */
    private int drawGraph(int centerX, int topY) {
        int width = Math.min(this.width - 80, 220);
        int height = 70;
        int left = centerX - width / 2;
        int bottom = topY + height;
        Gui.drawRect(left, topY, left + width, bottom, 0x44000000);
        if (valuationHistory.length == 0) {
            return bottom + 12;
        }
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        int count = valuationHistory.length;
        int start = Math.max(0, count - 26);
        int samples = count - start;
        if (samples <= 0) samples = count;
        for (int i = start; i < count; i++) {
            long val = valuationHistory[i];
            if (val > max) max = val;
            if (val < min) min = val;
        }
        if (min == Long.MAX_VALUE) {
            min = 0L;
        }
        if (max == Long.MIN_VALUE) {
            max = min;
        }
        boolean flat = max == min;
        float range = flat ? 1f : (float)(max - min);
        float stepX = samples > 1 ? (float)width / (samples - 1) : width;
        int prevX = -1;
        int prevY = -1;
        for (int idx = 0; idx < samples; idx++) {
            long val = valuationHistory[start + idx];
            float normalized = flat ? 0.5f : (val - min) / range;
            int x = left + Math.round(stepX * idx);
            int y = bottom - Math.round(normalized * (height - 6)) - 3;
            Gui.drawRect(x - 1, y - 1, x + 1, y + 1, 0xFF66CCFF);
            if (prevX >= 0) {
                drawVerticalQuad(prevX, prevY, x, y, bottom);
            }
            prevX = x;
            prevY = y;
        }
        String caption = I18n.format("primebank.market.details.graph_caption", samples);
        drawCenteredScaledString(caption, centerX, bottom + 6, 0xAAAAAA, width);
        return bottom + 12;
    }

    private void drawVerticalQuad(int x1, int y1, int x2, int y2, int bottom) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        if (maxX <= minX) maxX = minX + 1;
        Gui.drawRect(minX, minY, maxX, maxY, 0x8866CCFF);
        Gui.drawRect(minX, Math.min(bottom, Math.max(maxY, minY)), maxX, bottom, 0x224499FF);
    }

    /*
     English: Show percentage change and last valuation timestamp information.
     Español: Mostrar el cambio porcentual y la información de la última valoración.
    */
    private void drawHistorySummary(int centerX, int startY, int maxWidth) {
        if (valuationHistory == null || valuationHistory.length < 2) return;
        long latest = valuationHistory[valuationHistory.length - 1];
        long oldest = valuationHistory[Math.max(0, valuationHistory.length - 26)];
        String pct = formatChangePercent(oldest, latest);
        String changeLine = I18n.format("primebank.market.details.history_change", pct);
        drawCenteredScaledString(changeLine, centerX, startY, 0xBBBBBB, maxWidth);
    }

    private String formatChangePercent(long base, long current) {
        if (base <= 0L) {
            return current > 0L ? "+∞" : "0%";
        }
        double change = ((double) current - (double) base) / (double) base * 100.0;
        return String.format("%+.2f%%", change);
    }

    /*
     English: Format how long ago the details were fetched (seconds/minutes/hours).
     Español: Formatear cuánto tiempo ha pasado desde que se obtuvieron los detalles (segundos/minutos/horas).
    */
    private String formatSince(long timestampMs) {
        long delta = Math.max(0L, System.currentTimeMillis() - timestampMs);
        long seconds = delta / 1000L;
        if (seconds < 60L) return I18n.format("primebank.time.seconds", seconds);
        long minutes = seconds / 60L;
        if (minutes < 60L) return I18n.format("primebank.time.minutes", minutes);
        long hours = minutes / 60L;
        if (hours < 24L) return I18n.format("primebank.time.hours", hours);
        long days = hours / 24L;
        return I18n.format("primebank.time.days", days);
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
        int maxTop = this.height - 72;
        int safeTop = Math.max(defaultTop, contentBottomY + 12);
        if (safeTop > maxTop) safeTop = maxTop;
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

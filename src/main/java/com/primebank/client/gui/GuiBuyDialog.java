package com.primebank.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import com.primebank.PrimeBankMod;
import com.primebank.core.Money;
import com.primebank.market.MarketPrimaryService;
import com.primebank.net.PacketMarketBuy;

/*
 English: Dialog to enter number of shares to buy. Shows price and listed availability. Enforces basic constraints client-side.
 Español: Diálogo para ingresar número de acciones a comprar. Muestra precio y disponibilidad listada. Aplica restricciones básicas en el cliente.
*/
public class GuiBuyDialog extends GuiScreen {
    private final GuiCompanyDetails parent;
    private final String companyId;
    private final String displayName;
    private final long pricePerShareCents;
    private final int listedShares;

    private GuiTextField txtShares;
    private GuiButton btnConfirm;
    private GuiButton btnCancel;

    private final String buyerFeePercentLabel = formatBps(MarketPrimaryService.BUYER_FEE_BPS);
    private final String issuerFeePercentLabel = formatBps(MarketPrimaryService.ISSUER_FEE_BPS);

    public GuiBuyDialog(GuiCompanyDetails parent, String companyId, String displayName, long pricePerShareCents, int listedShares) {
        this.parent = parent;
        this.companyId = companyId;
        this.displayName = displayName;
        this.pricePerShareCents = pricePerShareCents;
        this.listedShares = listedShares;
    }

    @Override
    public void initGui() {
        int midX = this.width / 2;
        int midY = this.height / 2;
        this.buttonList.clear();
        this.txtShares = new GuiTextField(0, this.fontRenderer, midX - 60, midY, 120, 20);
        this.txtShares.setMaxStringLength(6);
        this.txtShares.setText("1");
        this.btnConfirm = new GuiButton(1, midX - 100, midY + 30, 98, 20, I18n.format("primebank.market.buydialog.buy"));
        this.btnCancel = new GuiButton(2, midX + 2, midY + 30, 98, 20, I18n.format("ui.primebank.cancel"));
        this.buttonList.add(btnConfirm);
        this.buttonList.add(btnCancel);
        updateButtonState();
    }

    private void updateButtonState() {
        int shares = parseShares();
        boolean valid = shares > 0 && shares <= Math.max(0, listedShares);
        btnConfirm.enabled = valid && pricePerShareCents > 0;
    }

    private int parseShares() {
        try {
            return Integer.parseInt(txtShares.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int midX = this.width / 2;
        int topY = this.height / 2 - 40;
        String title = I18n.format("primebank.market.buydialog.title", displayName);
        drawCenteredString(this.fontRenderer, title, midX, topY, 0xFFFFFF);
        topY += 14;
        String price = I18n.format("primebank.market.buydialog.price_each", Money.formatUsd(pricePerShareCents));
        drawCenteredString(this.fontRenderer, price, midX, topY, 0xDDDDDD);
        topY += 12;
        String listed = I18n.format("primebank.market.buydialog.listed_available", listedShares);
        drawCenteredString(this.fontRenderer, listed, midX, topY, 0xDDDDDD);
        topY += 18;
        String prompt = I18n.format("primebank.market.buydialog.enter_shares");
        drawCenteredString(this.fontRenderer, prompt, midX, topY, 0xDDDDDD);
        this.txtShares.drawTextBox();
        topY += 26;
        int shares = parseShares();
        long gross = calcGross(shares);
        long buyerFee = calcBuyerFee(gross);
        long total = calcTotal(gross, buyerFee);
        long issuerReceives = calcIssuerReceives(gross);
        String grossLine = I18n.format("primebank.market.buydialog.gross", shares, Money.formatUsd(gross));
        drawCenteredString(this.fontRenderer, grossLine, midX, topY, 0xCCCCCC); topY += 12;
        String buyerFeeLine = I18n.format("primebank.market.buydialog.buyer_fee", buyerFeePercentLabel, Money.formatUsd(buyerFee));
        drawCenteredString(this.fontRenderer, buyerFeeLine, midX, topY, 0xCCCCCC); topY += 12;
        String totalLine = I18n.format("primebank.market.buydialog.total", Money.formatUsd(total));
        drawCenteredString(this.fontRenderer, totalLine, midX, topY, 0xFFFFFF); topY += 12;
        String issuerLine = I18n.format("primebank.market.buydialog.issuer_receives", issuerFeePercentLabel, Money.formatUsd(issuerReceives));
        drawCenteredString(this.fontRenderer, issuerLine, midX, topY, 0x99FF99);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        this.txtShares.textboxKeyTyped(typedChar, keyCode);
        updateButtonState();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.txtShares.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnConfirm) {
            int shares = parseShares();
            if (shares > 0 && shares <= listedShares) {
                // English: Send buy request to server.
                // Español: Enviar solicitud de compra al servidor.
                PrimeBankMod.NETWORK.sendToServer(new PacketMarketBuy(companyId, shares));
            }
            // English: Return to details screen (it will refresh via details packet on success).
            // Español: Volver a la pantalla de detalles (se actualizará con el paquete de detalles si tiene éxito).
            Minecraft.getMinecraft().displayGuiScreen(parent);
        } else if (button == btnCancel) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    /*
     English: Calculate gross amount for the entered number of shares.
     Español: Calcular el monto bruto para el número de acciones ingresado.
    */
    private long calcGross(int shares) {
        if (shares <= 0) return 0L;
        return Math.multiplyExact((long) shares, pricePerShareCents);
    }

    /*
     English: Calculate buyer fee (bps) on the gross amount.
     Español: Calcular la comisión del comprador (bps) sobre el monto bruto.
    */
    private long calcBuyerFee(long gross) {
        if (gross <= 0) return 0L;
        return Money.multiplyBps(gross, MarketPrimaryService.BUYER_FEE_BPS);
    }

    /*
     English: Calculate issuer fee (bps) on the gross amount to determine company payout.
     Español: Calcular la comisión del emisor (bps) sobre el monto bruto para determinar el pago a la empresa.
    */
    private long calcIssuerReceives(long gross) {
        if (gross <= 0) return 0L;
        long issuerFee = Money.multiplyBps(gross, MarketPrimaryService.ISSUER_FEE_BPS);
        return Money.add(gross, -issuerFee);
    }

    /*
     English: Calculate total buyer debit (gross + buyer fee).
     Español: Calcular el débito total del comprador (bruto + comisión del comprador).
    */
    private long calcTotal(long gross, long buyerFee) {
        if (gross <= 0) return 0L;
        return Money.add(gross, buyerFee);
    }

    /*
     English: Format basis points as a percentage string with two decimals.
     Español: Formatear puntos básicos como cadena porcentual con dos decimales.
    */
    private static String formatBps(int bps) {
        return String.format("%.2f%%", bps / 100.0);
    }
}

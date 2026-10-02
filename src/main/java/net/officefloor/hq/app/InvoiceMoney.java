package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one place the invoice discount money-rule lives: given an invoice's subtotal (the worked-out
 * sum of its line items, kept in the amount column) and its percentage discount (Flyway V27), works
 * out the discount in money and the net total actually billed (subtotal less that discount). Kept in
 * one place so every surface that shows "what is owed" — the invoice detail, the client statement
 * and the home dashboard — discounts the same way, with the same whole-cent rounding.
 */
public final class InvoiceMoney {

    private InvoiceMoney() {
    }

    /** The discount in money: the subtotal times the percentage, rounded to whole cents. Null-safe. */
    public static BigDecimal discount(BigDecimal subtotal, BigDecimal discountPct) {
        BigDecimal base = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal pct = discountPct != null ? discountPct : BigDecimal.ZERO;
        return base.multiply(pct)
                .divide(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** The net total billed: the subtotal less the discount. Null-safe. */
    public static BigDecimal netTotal(BigDecimal subtotal, BigDecimal discountPct) {
        BigDecimal base = subtotal != null ? subtotal : BigDecimal.ZERO;
        return base.subtract(discount(base, discountPct));
    }
}

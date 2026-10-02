package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one place the invoice money-rules live: given an invoice's subtotal (the worked-out sum of its
 * line items, kept in the amount column), its percentage discount (Flyway V27) and its percentage
 * sales tax (Flyway V28), works out the discount in money, the tax in money and the final total
 * actually billed. Tax is added on top AFTER the discount: the total is (subtotal less discount) plus
 * tax on that net figure. Kept in one place so every surface that shows "what is owed" — the invoice
 * detail, the client statement and the home dashboard — works it out the same way, with the same
 * whole-cent rounding.
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

    /** The net figure the tax is worked out on: the subtotal less the discount. Null-safe. */
    public static BigDecimal netOfDiscount(BigDecimal subtotal, BigDecimal discountPct) {
        BigDecimal base = subtotal != null ? subtotal : BigDecimal.ZERO;
        return base.subtract(discount(base, discountPct));
    }

    /**
     * The tax in money: the net figure (subtotal less discount) times the tax percentage, rounded to
     * whole cents. Worked out AFTER the discount, so it never taxes the discounted-off amount.
     * Null-safe.
     */
    public static BigDecimal tax(BigDecimal subtotal, BigDecimal discountPct, BigDecimal taxPct) {
        BigDecimal net = netOfDiscount(subtotal, discountPct);
        BigDecimal pct = taxPct != null ? taxPct : BigDecimal.ZERO;
        return net.multiply(pct)
                .divide(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** The final total billed: the subtotal less the discount, plus tax added on top. Null-safe. */
    public static BigDecimal netTotal(BigDecimal subtotal, BigDecimal discountPct, BigDecimal taxPct) {
        return netOfDiscount(subtotal, discountPct).add(tax(subtotal, discountPct, taxPct));
    }
}

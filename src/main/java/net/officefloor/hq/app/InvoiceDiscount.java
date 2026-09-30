package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One invoice's discount picture: the {@code subtotal} (the sum of its line items, before any
 * discount), the percentage-based {@code discount} taken off it, and the {@code total} the client
 * actually owes ({@code subtotal - discount}). Derived per request in {@link InvoiceDiscountGet} from
 * the invoice's stored {@code discountPct} and its line items, so the three figures always agree and
 * nothing is stored twice. The discount is {@code subtotal * pct / 100}, rounded to the cent.
 */
public class InvoiceDiscount {

    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public InvoiceDiscount(BigDecimal subtotal, BigDecimal discountPct) {
        BigDecimal sub = subtotal == null ? BigDecimal.ZERO : subtotal;
        BigDecimal pct = discountPct == null ? BigDecimal.ZERO : discountPct;
        this.subtotal = sub;
        this.discount = sub.multiply(pct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.total = sub.subtract(this.discount);
    }

    /** The sum of the invoice's line items, before any discount. */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /** The money taken off as a discount: subtotal * pct / 100. */
    public BigDecimal getDiscount() {
        return discount;
    }

    /** What the client owes after the discount: subtotal - discount. */
    public BigDecimal getTotal() {
        return total;
    }
}

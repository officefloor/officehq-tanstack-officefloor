package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One invoice's money breakdown, derived per request from its line items and its stored
 * {@code discountPct} so the figures always agree and nothing is stored twice: the {@code subtotal}
 * (the sum of its line items, before any discount), the percentage-based {@code discount} taken off
 * it, and the {@code total} the client actually owes ({@code subtotal - discount}).
 *
 * <p>This is the single home for an invoice's money arithmetic. {@link #forLines} builds the whole
 * breakdown from an invoice's lines; {@link #subtotalOf} exposes just the line-item sum for callers
 * (the line-item endpoints) that only keep the invoice's stored amount in step. Percentages go
 * through {@link #pctOf}, rounded to the cent HALF_UP.
 */
public class InvoiceDiscount {

    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public InvoiceDiscount(BigDecimal subtotal, BigDecimal discountPct) {
        this.subtotal = orZero(subtotal);
        this.discount = pctOf(this.subtotal, discountPct);
        this.total = this.subtotal.subtract(this.discount);
    }

    /** The full breakdown for an invoice's lines: subtotal summed from them, then the discount. */
    public static InvoiceDiscount forLines(Iterable<LineItem> lines, BigDecimal discountPct) {
        return new InvoiceDiscount(subtotalOf(lines), discountPct);
    }

    /** The subtotal an invoice's lines add up to: the sum of each line's qty * unitPrice. */
    public static BigDecimal subtotalOf(Iterable<LineItem> lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem line : lines) {
            if (line.getUnitPrice() != null && line.getQty() != null) {
                subtotal = subtotal.add(
                        line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty())));
            }
        }
        return subtotal;
    }

    /** {@code base * pct / 100}, rounded to the cent HALF_UP; a null base or pct counts as zero. */
    private static BigDecimal pctOf(BigDecimal base, BigDecimal pct) {
        return orZero(base).multiply(orZero(pct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

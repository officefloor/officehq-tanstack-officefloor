package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the {@code /api/invoices/summary} route returns for one invoice: its {@code subtotal} (the sum
 * of qty * unit price across its line items), the {@code discountPct} taken off it, the resulting
 * {@code discount} amount, and the final {@code total} (subtotal minus the discount). All derived in
 * {@link InvoiceSummaryGet}, not stored — the invoice-detail summary panel reads these three money
 * figures so the UI does not recompute the discount itself.
 */
public class InvoiceSummaryView {

    private final BigDecimal subtotal;
    private final BigDecimal discountPct;
    private final BigDecimal discount;
    private final BigDecimal taxPct;
    private final BigDecimal tax;
    private final BigDecimal total;

    public InvoiceSummaryView(BigDecimal subtotal, BigDecimal discountPct, BigDecimal discount,
            BigDecimal taxPct, BigDecimal tax, BigDecimal total) {
        this.subtotal = subtotal;
        this.discountPct = discountPct;
        this.discount = discount;
        this.taxPct = taxPct;
        this.tax = tax;
        this.total = total;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTaxPct() {
        return taxPct;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getTotal() {
        return total;
    }
}

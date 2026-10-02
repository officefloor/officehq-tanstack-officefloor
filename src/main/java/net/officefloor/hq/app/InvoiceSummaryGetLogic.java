package net.officefloor.hq.app;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/summary?invoiceId=<id>} — the money breakdown for one invoice: its
 * subtotal (the worked-out sum of its line items, Flyway V13/V14, kept in the amount column), the
 * percentage discount taken off it (Flyway V27), the discount in money, the percentage sales tax
 * (Flyway V28), the tax in money, and the final total (subtotal minus discount, plus tax on top).
 * The figures are worked out on the server so the money math lives in one place; an invoice with no
 * discount or tax simply bills its full subtotal. Wired by
 * {@code officefloor/rest/api/invoices/summary.GET.yml}.
 */
public class InvoiceSummaryGetLogic {

    public void service(@RequestParam("invoiceId") Long invoiceId, InvoiceRepository invoices,
            ObjectResponse<InvoiceSummaryView> response) {
        Invoice invoice = invoices.findById(invoiceId).orElse(null);
        BigDecimal subtotal = invoice != null ? invoice.getAmount() : BigDecimal.ZERO;
        BigDecimal discountPct = invoice != null && invoice.getDiscountPct() != null
                ? invoice.getDiscountPct()
                : BigDecimal.ZERO;
        BigDecimal taxPct = invoice != null && invoice.getTaxPct() != null
                ? invoice.getTaxPct()
                : BigDecimal.ZERO;
        // Discount and tax in money, rounded to whole cents; the final total is the subtotal less the
        // discount, plus tax worked out on that net figure. The same one-place rule the dashboard,
        // statement and due figures use (see InvoiceMoney) — tax is added on top AFTER the discount.
        BigDecimal discount = InvoiceMoney.discount(subtotal, discountPct);
        BigDecimal tax = InvoiceMoney.tax(subtotal, discountPct, taxPct);
        BigDecimal total = InvoiceMoney.netTotal(subtotal, discountPct, taxPct);
        response.send(new InvoiceSummaryView(invoiceId, subtotal, discountPct, discount, taxPct, tax,
                total));
    }

    /**
     * One invoice's money breakdown: subtotal, the discount percentage, the discount, the tax
     * percentage, the tax, and the final total.
     */
    public static class InvoiceSummaryView {
        private final long invoiceId;
        private final BigDecimal subtotal;
        private final BigDecimal discountPct;
        private final BigDecimal discount;
        private final BigDecimal taxPct;
        private final BigDecimal tax;
        private final BigDecimal total;

        public InvoiceSummaryView(long invoiceId, BigDecimal subtotal, BigDecimal discountPct,
                BigDecimal discount, BigDecimal taxPct, BigDecimal tax, BigDecimal total) {
            this.invoiceId = invoiceId;
            this.subtotal = subtotal;
            this.discountPct = discountPct;
            this.discount = discount;
            this.taxPct = taxPct;
            this.tax = tax;
            this.total = total;
        }

        public long getInvoiceId() {
            return invoiceId;
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
}

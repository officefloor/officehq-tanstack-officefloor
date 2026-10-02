package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/summary?invoiceId=<id>} — the money breakdown for one invoice: its
 * subtotal (the worked-out sum of its line items, Flyway V13/V14, kept in the amount column), the
 * percentage discount taken off it (Flyway V27), the discount in money, and the final total
 * (subtotal minus discount). The figures are worked out on the server so the money math lives in one
 * place; an invoice with no discount simply bills its full subtotal. Wired by
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
        // Discount in money, rounded to whole cents; the final total is the subtotal less that.
        BigDecimal discount = subtotal.multiply(discountPct)
                .divide(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discount);
        response.send(new InvoiceSummaryView(invoiceId, subtotal, discountPct, discount, total));
    }

    /** One invoice's money breakdown: subtotal, the discount percentage, the discount, the total. */
    public static class InvoiceSummaryView {
        private final long invoiceId;
        private final BigDecimal subtotal;
        private final BigDecimal discountPct;
        private final BigDecimal discount;
        private final BigDecimal total;

        public InvoiceSummaryView(long invoiceId, BigDecimal subtotal, BigDecimal discountPct,
                BigDecimal discount, BigDecimal total) {
            this.invoiceId = invoiceId;
            this.subtotal = subtotal;
            this.discountPct = discountPct;
            this.discount = discount;
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

        public BigDecimal getTotal() {
            return total;
        }
    }
}

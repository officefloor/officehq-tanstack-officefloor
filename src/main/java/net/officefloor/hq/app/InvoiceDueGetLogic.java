package net.officefloor.hq.app;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/due?invoiceId=<id>} — how much is still left to pay on one invoice: its
 * amount (the worked-out sum of its line items, Flyway V13/V14) minus everything paid against it (the
 * sum of its payments, Flyway V19). The figure is derived on the server so the money math lives in
 * one place, and an invoice with no payments yet is simply its full amount. Wired by
 * {@code officefloor/rest/api/invoices/due.GET.yml}.
 */
public class InvoiceDueGetLogic {

    public void service(@RequestParam("invoiceId") Long invoiceId, InvoiceRepository invoices,
            PaymentRepository payments, ObjectResponse<InvoiceDueView> response) {
        Invoice invoice = invoices.findById(invoiceId).orElse(null);
        BigDecimal subtotal = invoice != null ? invoice.getAmount() : BigDecimal.ZERO;
        // What is owed is the net total after any discount (Flyway V27) with tax added on top (Flyway
        // V28), worked out the one-place way (InvoiceMoney) so the detail figure agrees with the
        // dashboard and the statement.
        BigDecimal amount = InvoiceMoney.netTotal(subtotal,
                invoice != null ? invoice.getDiscountPct() : null,
                invoice != null ? invoice.getTaxPct() : null);
        BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoiceId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // The status is worked out from the payments too (see InvoiceStatus), so the detail page
        // reads the same one-place rule as the list — no hand-set paid flag. It compares against the
        // discounted total, so an invoice is PAID once the payments cover what is actually owed.
        String status = InvoiceStatus.derive(invoice != null ? invoice.getStatus() : null, amount,
                paid);
        response.send(new InvoiceDueView(invoiceId, amount, paid, amount.subtract(paid), status));
    }

    /**
     * What is left to pay on one invoice: its amount, what has been paid, the difference, and the
     * status worked out from the payments (SENT / PARTIAL / PAID).
     */
    public static class InvoiceDueView {
        private final long invoiceId;
        private final BigDecimal amount;
        private final BigDecimal paid;
        private final BigDecimal due;
        private final String status;

        public InvoiceDueView(long invoiceId, BigDecimal amount, BigDecimal paid, BigDecimal due,
                String status) {
            this.invoiceId = invoiceId;
            this.amount = amount;
            this.paid = paid;
            this.due = due;
            this.status = status;
        }

        public long getInvoiceId() {
            return invoiceId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getPaid() {
            return paid;
        }

        public BigDecimal getDue() {
            return due;
        }

        public String getStatus() {
            return status;
        }
    }
}

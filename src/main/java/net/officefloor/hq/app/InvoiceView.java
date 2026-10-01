package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for an invoice: the shape the front-end renders into the project's invoices
 * table. The amount keeps its scale (a {@link BigDecimal}) so the client formats it to two places.
 */
public record InvoiceView(Long id, Long projectId, BigDecimal amount, String status,
        String issuedDate, String dueDate) {

    public static InvoiceView of(Invoice invoice) {
        return new InvoiceView(invoice.getId(), invoice.getProjectId(), invoice.getAmount(),
                invoice.getStatus(), invoice.getIssuedDate().toString(),
                invoice.getDueDate().toString());
    }

    /**
     * The same view, but with the status WORKED OUT from what has been paid against the invoice (the
     * sum of its payments) rather than a figure flipped by hand. This is how the UI learns whether an
     * invoice is still owing, part paid or settled.
     */
    public static InvoiceView ofDerived(Invoice invoice, java.math.BigDecimal paidSum) {
        return new InvoiceView(invoice.getId(), invoice.getProjectId(), invoice.getAmount(),
                deriveStatus(invoice, paidSum), invoice.getIssuedDate().toString(),
                invoice.getDueDate().toString());
    }

    /**
     * Work out an invoice's status from its payments: PAID once the payments cover its amount,
     * PARTIAL once some has been paid but not all, otherwise the invoice's own lifecycle status
     * (DRAFT before it goes out, SENT once it has). A zero-amount invoice is never PAID off nothing.
     */
    static String deriveStatus(Invoice invoice, java.math.BigDecimal paidSum) {
        if (paidSum != null && paidSum.signum() > 0) {
            java.math.BigDecimal amount = invoice.getAmount();
            if (amount != null && amount.signum() > 0 && paidSum.compareTo(amount) >= 0) {
                return "PAID";
            }
            return "PARTIAL";
        }
        return invoice.getStatus();
    }
}

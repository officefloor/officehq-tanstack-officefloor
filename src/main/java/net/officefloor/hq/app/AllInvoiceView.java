package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for an invoice in the cross-project list: the shape the "all invoices" page
 * renders. The cross-entity join lives here — each invoice carries its project's NAME (not just the
 * id) so the one list can show which project the invoice is for without a second lookup. The amount
 * keeps its {@link BigDecimal} scale so the client formats it to two places.
 */
public record AllInvoiceView(Long id, Long projectId, String projectName, String status,
        BigDecimal amount) {

    public static AllInvoiceView of(Invoice invoice, String projectName) {
        return new AllInvoiceView(invoice.getId(), invoice.getProjectId(), projectName,
                invoice.getStatus(), invoice.getAmount());
    }
}

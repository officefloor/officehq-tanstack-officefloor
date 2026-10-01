package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for an invoice: the shape the front-end renders into the project's invoices
 * table. The amount keeps its scale (a {@link BigDecimal}) so the client formats it to two places.
 */
public record InvoiceView(Long id, Long projectId, BigDecimal amount) {

    public static InvoiceView of(Invoice invoice) {
        return new InvoiceView(invoice.getId(), invoice.getProjectId(), invoice.getAmount());
    }
}

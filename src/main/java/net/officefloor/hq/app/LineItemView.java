package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for one invoice line: the shape the front-end renders into the invoice's line
 * items table. The unit price keeps its scale (a {@link BigDecimal}) so the client formats money to
 * two places; the client derives each line's amount and the invoice total from qty and unit price.
 */
public record LineItemView(Long id, Long invoiceId, String description, int qty, String unit,
        BigDecimal unitPrice) {

    public static LineItemView of(InvoiceLineItem item) {
        return new LineItemView(item.getId(), item.getInvoiceId(), item.getDescription(),
                item.getQuantity(), item.getUnit(), item.getUnitPrice());
    }
}

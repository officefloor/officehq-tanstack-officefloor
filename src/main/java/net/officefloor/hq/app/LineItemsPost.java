package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/lineitems} — add a line (description, qty, unit price) to an invoice and return
 * the created row. Wired by {@code officefloor/rest/api/lineitems.POST.yml}. The invoice must exist,
 * the description must be non-blank, and qty and unit price must be sensible. Adding a line changes
 * the invoice's total, so the invoice's derived {@code amount} is recomputed from all its lines and
 * stored — that is what the invoice list and dashboard read. The add is audited through {@link
 * Audit}.
 */
public class LineItemsPost {

    public void service(@RequestBody NewLineItem body, LineItemRepository lineItems,
            InvoiceRepository invoices, Audit audit, ObjectResponse<LineItem> response) {
        Long invoiceId = body.getInvoiceId();
        Invoice invoice = invoiceId == null ? null : invoices.findById(invoiceId).orElse(null);
        if (invoice == null) {
            throw new IllegalArgumentException("a line item requires an existing invoice");
        }
        String description = body.getDescription() == null ? "" : body.getDescription().trim();
        if (description.isEmpty()) {
            throw new IllegalArgumentException("a line item requires a description");
        }
        Integer qty = body.getQty();
        if (qty == null || qty <= 0) {
            throw new IllegalArgumentException("a line item requires a positive quantity");
        }
        BigDecimal unitPrice = body.getUnitPrice();
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("a line item requires a non-negative unit price");
        }
        LineItem item = new LineItem();
        item.setInvoiceId(invoiceId);
        item.setDescription(description);
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        LineItem saved = lineItems.save(item);

        // Recompute the invoice's derived amount from all its lines and store it, so the invoice
        // list (/api/invoices) and the dashboard read a correct total without re-summing.
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem li : lineItems.findByInvoiceIdOrderByIdAsc(invoiceId)) {
            total = total.add(li.getUnitPrice().multiply(BigDecimal.valueOf(li.getQty())));
        }
        invoice.setAmount(total);
        invoices.save(invoice);

        audit.record("LINE_ITEM_ADDED id=" + saved.getId() + " invoice=" + invoiceId
                + " description=" + saved.getDescription() + " qty=" + saved.getQty()
                + " unitPrice=" + saved.getUnitPrice().toPlainString());
        response.send(saved);
    }
}

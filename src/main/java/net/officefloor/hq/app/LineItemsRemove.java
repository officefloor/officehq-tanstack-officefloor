package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/lineitems/remove} — take a line off its invoice and return the invoice's updated
 * total. Wired by {@code officefloor/rest/api/lineitems/remove.POST.yml}. Removing a line changes the
 * invoice's total, so the invoice's derived {@code amount} is recomputed from its remaining lines and
 * stored — that is what the invoice list and dashboard read. The removal is audited through
 * {@link Audit}.
 */
public class LineItemsRemove {

    public void service(@RequestBody RemoveLineItem body, LineItemRepository lineItems,
            InvoiceRepository invoices, Audit audit, ObjectResponse<Invoice> response) {
        Long id = body.getId();
        LineItem item = id == null ? null : lineItems.findById(id).orElse(null);
        if (item == null) {
            throw new IllegalArgumentException("no such line item");
        }
        Long invoiceId = item.getInvoiceId();
        lineItems.delete(item);

        Invoice invoice = invoices.findById(invoiceId).orElseThrow(
                () -> new IllegalArgumentException("a line item requires an existing invoice"));
        // Recompute the invoice's derived amount from its remaining lines and store it, so the
        // invoice list (/api/invoices) and the dashboard read a correct total without re-summing.
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem li : lineItems.findByInvoiceIdOrderByIdAsc(invoiceId)) {
            total = total.add(li.getUnitPrice().multiply(BigDecimal.valueOf(li.getQty())));
        }
        invoice.setAmount(total);
        invoices.save(invoice);

        audit.record("LINE_ITEM_REMOVED id=" + id + " invoice=" + invoiceId
                + " amount=" + total.toPlainString());
        response.send(invoice);
    }
}

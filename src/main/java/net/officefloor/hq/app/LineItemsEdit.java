package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/lineitems/edit} — change a line's description, quantity and unit price and return
 * the updated row. Wired by {@code officefloor/rest/api/lineitems/edit.POST.yml}. The same validation
 * as adding applies (non-blank description, positive qty, non-negative unit price). Changing a line
 * changes the invoice's total, so the invoice's derived {@code amount} is recomputed from all its
 * lines and stored. The change is audited through {@link Audit}.
 */
public class LineItemsEdit {

    public void service(@RequestBody EditLineItem body, LineItemRepository lineItems,
            InvoiceRepository invoices, Audit audit, ObjectResponse<LineItem> response) {
        Long id = body.getId();
        LineItem item = id == null ? null : lineItems.findById(id).orElse(null);
        if (item == null) {
            throw new IllegalArgumentException("no such line item");
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
        String unit = body.getUnit() == null || body.getUnit().trim().isEmpty()
                ? "unit" : body.getUnit().trim();
        item.setDescription(description);
        item.setQty(qty);
        item.setUnit(unit);
        item.setUnitPrice(unitPrice);
        LineItem saved = lineItems.save(item);

        Long invoiceId = saved.getInvoiceId();
        Invoice invoice = invoices.findById(invoiceId).orElseThrow(
                () -> new IllegalArgumentException("a line item requires an existing invoice"));
        // Recompute the invoice's derived amount from all its lines and store it, so the invoice
        // list (/api/invoices) and the dashboard read a correct total without re-summing.
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem li : lineItems.findByInvoiceIdOrderByIdAsc(invoiceId)) {
            total = total.add(li.getUnitPrice().multiply(BigDecimal.valueOf(li.getQty())));
        }
        invoice.setAmount(total);
        invoices.save(invoice);

        audit.record("LINE_ITEM_UPDATED id=" + saved.getId() + " invoice=" + invoiceId
                + " description=" + saved.getDescription() + " qty=" + saved.getQty()
                + " unit=" + saved.getUnit()
                + " unitPrice=" + saved.getUnitPrice().toPlainString());
        response.send(saved);
    }
}

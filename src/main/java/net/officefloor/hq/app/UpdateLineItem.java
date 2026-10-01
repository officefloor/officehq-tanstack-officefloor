package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/lineitems/update — change a line item on an invoice from the submitted
 * description, quantity and unit price, returning the updated line. Wired by
 * officefloor/rest/api/invoices/lineitems/update.POST.yml.
 *
 * A changed line must still be for something: we reject a missing/unknown line id, a blank
 * description, a quantity that is not more than zero, or a missing/negative unit price before
 * persisting so a bad line can never be saved — the same guards CreateLineItem applies on add. After
 * changing the line we recompute the invoice's amount as the SUM of all its lines, so the amount the
 * invoices list shows stays the derived total.
 */
public class UpdateLineItem {

    public void service(@RequestBody UpdateLineItemForm form, InvoiceRepository invoices,
            InvoiceLineItemRepository lineItems, ObjectResponse<LineItemView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A line item id is required");
        }
        InvoiceLineItem item = lineItems.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid line item is required"));
        String description = form.getDescription();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A line item description is required");
        }
        Integer qty = form.getQty();
        if (qty == null || qty <= 0) {
            throw new IllegalArgumentException("A line item quantity must be greater than zero");
        }
        BigDecimal unitPrice = form.getUnitPrice();
        if (unitPrice == null) {
            throw new IllegalArgumentException("A line item unit price is required");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("A line item unit price cannot be negative");
        }
        item.update(description.trim(), qty, unitPrice);
        InvoiceLineItem saved = lineItems.save(item);

        Long invoiceId = saved.getInvoiceId();
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        List<InvoiceLineItem> lines = lineItems.findByInvoiceIdOrderByIdAsc(invoiceId);
        BigDecimal total = lines.stream().map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmount(total);
        invoices.save(invoice);

        response.send(LineItemView.of(saved));
    }
}

package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/lineitems — add a line item to an invoice from the submitted description,
 * quantity and unit price, returning the saved line (with its generated id). Wired by
 * officefloor/rest/api/invoices/lineitems.POST.yml.
 *
 * A line must name a real invoice and be for something: we reject a missing/unknown invoice id, a
 * blank description, a quantity that is not more than zero, or a missing/negative unit price before
 * persisting so a bad line can never be saved. After adding the line we recompute the invoice's
 * amount as the SUM of all its lines, so the amount the invoices list shows stays the derived total.
 */
public class CreateLineItem {

    public void service(@RequestBody LineItemForm form, InvoiceRepository invoices,
            InvoiceLineItemRepository lineItems, ObjectResponse<LineItemView> response) {
        Long invoiceId = form.getInvoiceId();
        if (invoiceId == null) {
            throw new IllegalArgumentException("An invoice id is required");
        }
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
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
        InvoiceLineItem saved =
                lineItems.save(new InvoiceLineItem(invoiceId, description.trim(), qty, unitPrice));

        // The invoice amount is the sum of its lines — recompute and persist it so every view that
        // reads the stored amount (the project's invoices list) stays in step with the lines.
        List<InvoiceLineItem> lines = lineItems.findByInvoiceIdOrderByIdAsc(invoiceId);
        BigDecimal total = lines.stream().map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmount(total);
        invoices.save(invoice);

        response.send(LineItemView.of(saved));
    }
}

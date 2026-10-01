package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/lineitems/remove — drop a line item from an invoice, returning the line that was
 * removed. Wired by officefloor/rest/api/invoices/lineitems/remove.POST.yml.
 *
 * We reject a missing or unknown line id before writing anything so a bad request neither changes
 * state nor returns a row. After removing the line we recompute the invoice's amount as the SUM of
 * its remaining lines (the same derivation CreateLineItem keeps in step), so every view that reads
 * the stored amount — the project's invoices list — stays in step with the lines.
 */
public class RemoveLineItem {

    public void service(@RequestBody RemoveLineItemForm form, InvoiceRepository invoices,
            InvoiceLineItemRepository lineItems, ObjectResponse<LineItemView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A line item id is required");
        }
        InvoiceLineItem item = lineItems.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid line item is required"));
        Long invoiceId = item.getInvoiceId();
        lineItems.delete(item);

        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        List<InvoiceLineItem> lines = lineItems.findByInvoiceIdOrderByIdAsc(invoiceId);
        BigDecimal total = lines.stream().map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmount(total);
        invoices.save(invoice);

        response.send(LineItemView.of(item));
    }
}

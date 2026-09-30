package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/invoices/{id}/lineitems/{lineItemId}/remove — remove one line from an invoice and
 * return the updated invoice. Wired by
 * {@code officefloor/rest/api/invoices/{id}/lineitems/{lineItemId}/remove.POST.yml}. An unknown
 * invoice or line is rejected with 404, and a line that does not belong to the named invoice is
 * likewise 404. The invoice's stored amount is kept in step as the sum of its remaining lines'
 * qty * unitPrice, so every view that shows the amount stays correct after a removal.
 */
public class InvoiceLineItemRemove {

    public void service(@HttpPathParameter("id") String id,
            @HttpPathParameter("lineItemId") String lineItemId, InvoiceRepository invoices,
            LineItemRepository lineItems, ObjectResponse<Invoice> response) {
        Long invoiceId = Long.valueOf(id);
        Long lineId = Long.valueOf(lineItemId);
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        LineItem line = lineItems.findById(lineId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        if (!invoiceId.equals(line.getInvoiceId())) {
            throw new HttpException(HttpStatus.NOT_FOUND);
        }
        lineItems.delete(line);

        // Keep the invoice's stored amount in step with its lines: amount = sum(qty * unitPrice).
        invoice.setAmount(
                InvoiceDiscount.subtotalOf(lineItems.findByInvoiceIdOrderByIdAsc(invoiceId)));
        invoices.save(invoice);

        response.send(invoice);
    }
}

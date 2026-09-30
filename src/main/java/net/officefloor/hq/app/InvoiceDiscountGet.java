package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{id}/discount — one invoice's discount breakdown: the subtotal (the sum of its
 * line items), the percentage discount taken off it, and the final total (subtotal - discount).
 * Derived per request from the invoice's stored {@code discountPct} and its line items, so it always
 * reflects the current lines and never stores the total twice. The arithmetic itself lives in
 * {@link InvoiceDiscount}. Wired by {@code officefloor/rest/api/invoices/{id}/discount.GET.yml}. An
 * unknown invoice id is 404.
 */
public class InvoiceDiscountGet {

    public void service(@HttpPathParameter("id") String id, InvoiceRepository invoices,
            LineItemRepository lineItems, ObjectResponse<InvoiceDiscount> response) {
        Long invoiceId = Long.valueOf(id);
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        List<LineItem> lines = lineItems.findByInvoiceIdOrderByIdAsc(invoiceId);
        response.send(InvoiceDiscount.forLines(lines, invoice.getDiscountPct()));
    }
}

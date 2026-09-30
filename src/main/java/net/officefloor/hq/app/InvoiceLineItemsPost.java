package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/{id}/lineitems — add a line to an invoice from {description, qty, unitPrice}
 * and return the saved row (with its id). Wired by
 * {@code officefloor/rest/api/invoices/{id}/lineitems.POST.yml}. A line needs a description, a
 * whole quantity above zero and a unit price above zero; anything missing/invalid is rejected with
 * 400 and an unknown invoice with 404. The invoice's stored amount is kept in step as the sum of
 * its lines' qty * unitPrice, so every view that shows the amount stays correct.
 */
public class InvoiceLineItemsPost {

    public void service(@HttpPathParameter("id") String id, @RequestBody NewLineItem body,
            InvoiceRepository invoices, LineItemRepository lineItems,
            ObjectResponse<LineItem> response) {
        Long invoiceId = Long.valueOf(id);
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        String description = body.getDescription();
        Integer qty = body.getQty();
        String unit = body.getUnit();
        BigDecimal unitPrice = body.getUnitPrice();
        if (description == null || description.isBlank() || qty == null || qty <= 0
                || unit == null || unit.isBlank() || unitPrice == null || unitPrice.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        LineItem item = new LineItem();
        item.setInvoiceId(invoiceId);
        item.setDescription(description.trim());
        item.setQty(qty);
        item.setUnit(unit.trim());
        item.setUnitPrice(unitPrice);
        LineItem saved = lineItems.save(item);

        // Keep the invoice's stored amount in step with its lines: amount = sum(qty * unitPrice).
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem line : lineItems.findByInvoiceIdOrderByIdAsc(invoiceId)) {
            total = total.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty())));
        }
        invoice.setAmount(total);
        invoices.save(invoice);

        response.send(saved);
    }
}

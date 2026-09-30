package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/{id}/lineitems/{lineItemId} — change one line on an invoice from
 * {description, qty, unitPrice} and return the saved row. Wired by
 * {@code officefloor/rest/api/invoices/{id}/lineitems/{lineItemId}.POST.yml}. Same validation as
 * adding a line: a description, a whole quantity above zero and a unit price above zero; anything
 * missing/invalid is rejected with 400, an unknown invoice or line with 404, and a line that does
 * not belong to the named invoice likewise with 404. The invoice's stored amount is kept in step as
 * the sum of its lines' qty * unitPrice, so every view that shows the amount stays correct.
 */
public class InvoiceLineItemUpdate {

    public void service(@HttpPathParameter("id") String id,
            @HttpPathParameter("lineItemId") String lineItemId, @RequestBody NewLineItem body,
            InvoiceRepository invoices, LineItemRepository lineItems,
            ObjectResponse<LineItem> response) {
        Long invoiceId = Long.valueOf(id);
        Long lineId = Long.valueOf(lineItemId);
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        LineItem line = lineItems.findById(lineId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        if (!invoiceId.equals(line.getInvoiceId())) {
            throw new HttpException(HttpStatus.NOT_FOUND);
        }
        String description = body.getDescription();
        Integer qty = body.getQty();
        BigDecimal unitPrice = body.getUnitPrice();
        if (description == null || description.isBlank() || qty == null || qty <= 0
                || unitPrice == null || unitPrice.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        line.setDescription(description.trim());
        line.setQty(qty);
        // Unit is optional on an edit: when the body supplies one, update it; otherwise keep the
        // line's existing unit rather than blanking a value the editor did not touch.
        if (body.getUnit() != null && !body.getUnit().isBlank()) {
            line.setUnit(body.getUnit().trim());
        }
        line.setUnitPrice(unitPrice);
        LineItem saved = lineItems.save(line);

        // Keep the invoice's stored amount in step with its lines: amount = sum(qty * unitPrice).
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem l : lineItems.findByInvoiceIdOrderByIdAsc(invoiceId)) {
            total = total.add(l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQty())));
        }
        invoice.setAmount(total);
        invoices.save(invoice);

        response.send(saved);
    }
}

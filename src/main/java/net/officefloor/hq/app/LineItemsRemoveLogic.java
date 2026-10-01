package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/lineitems/remove} — take a charge line off an invoice from an {id} body and return
 * the updated invoice. Wired by {@code officefloor/rest/api/lineitems/remove.POST.yml}. The line must
 * exist (404 otherwise). Removing a line re-works the invoice's amount (the sum of qty * unitPrice
 * across the lines that remain, zero when none are left) and stores it, so the total the UI shows is
 * always worked out from the lines, never typed — the mirror of {@link LineItemsPostLogic}.
 */
public class LineItemsRemoveLogic {

    public void service(@RequestBody RemoveLineItem body, LineItemRepository lineItems,
            InvoiceRepository invoices, ObjectResponse<Invoice> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A line item id is required");
        }
        LineItem line = lineItems.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such line item"));
        Long invoiceId = line.getInvoiceId();
        lineItems.delete(line);

        // Re-work the invoice's amount from the lines that remain and store it, so the figure stays
        // derived — the sum drops by exactly the removed line's amount.
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such invoice"));
        List<LineItem> lines = lineItems.findByInvoiceIdOrderByIdAsc(invoiceId);
        BigDecimal total = lines.stream().map(LineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmount(total);
        Invoice saved = invoices.save(invoice);

        response.send(saved);
    }

    /** Request body for removing a line item from an invoice. */
    public static class RemoveLineItem {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}

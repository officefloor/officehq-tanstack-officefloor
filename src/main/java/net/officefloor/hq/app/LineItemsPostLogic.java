package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/lineitems} — add a line item to an invoice from a
 * {invoiceId, description, qty, unitPrice} body and return the saved row (with its generated id).
 * Wired by {@code officefloor/rest/api/lineitems.POST.yml}. A line must belong to an existing
 * invoice, carry a non-blank description, a positive quantity and a positive unit price; all are
 * validated here and rejected with 400. Adding a line re-works the invoice's amount (the sum of
 * qty * unitPrice across its lines) and stores it, so the total the UI shows is always worked out,
 * never typed.
 */
public class LineItemsPostLogic {

    public void service(@RequestBody NewLineItem body, LineItemRepository lineItems,
            InvoiceRepository invoices, ObjectResponse<LineItem> response) {
        Long invoiceId = body.getInvoiceId();
        if (invoiceId == null || !invoices.existsById(invoiceId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A valid invoice is required");
        }
        String description = body.getDescription();
        if (description == null || description.isBlank()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A description is required");
        }
        Integer qty = body.getQty();
        if (qty == null || qty <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A positive quantity is required");
        }
        BigDecimal unitPrice = body.getUnitPrice();
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A positive unit price is required");
        }
        // The unit a line is measured in (e.g. "hours"); optional, defaulting to "units" when blank.
        String unit = body.getUnit();
        unit = (unit == null || unit.isBlank()) ? "units" : unit.trim();

        LineItem saved =
                lineItems.save(new LineItem(invoiceId, description.trim(), qty, unit, unitPrice));

        // Re-work the invoice's amount from its lines and store it, so the figure is derived.
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such invoice"));
        List<LineItem> lines = lineItems.findByInvoiceIdOrderByIdAsc(invoiceId);
        BigDecimal total = lines.stream().map(LineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmount(total);
        invoices.save(invoice);

        response.send(saved);
    }

    /** Request body for adding a line item to an invoice. */
    public static class NewLineItem {
        private Long invoiceId;
        private String description;
        private Integer qty;
        private String unit;
        private BigDecimal unitPrice;

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}

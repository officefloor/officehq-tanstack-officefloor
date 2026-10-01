package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/summary?invoiceId=&lt;id&gt; — an invoice's money summary: its SUBTOTAL (the sum
 * of its line items' amounts), the percentage DISCOUNT set on it and the DISCOUNT amount that works
 * out to, the percentage sales TAX set on it and the TAX amount that works out to, and the final
 * TOTAL. Tax is applied after the discount, so the total is the subtotal minus the discount, plus the
 * tax on what is left. Scoped to one invoice, so the invoice id arrives as a query parameter. Derived
 * on the server in {@link BigDecimal} (money scale, no float drift). Wired by
 * officefloor/rest/api/invoices/summary.GET.yml.
 */
public class GetInvoiceSummary {

    public void service(@RequestParam("invoiceId") String invoiceId, InvoiceRepository invoices,
            InvoiceLineItemRepository lineItems, ObjectResponse<InvoiceSummaryView> response) {
        Long id = Long.valueOf(invoiceId);
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        BigDecimal subtotal = lineItems.findByInvoiceIdOrderByIdAsc(id).stream()
                .map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountPct =
                invoice.getDiscountPct() == null ? BigDecimal.ZERO : invoice.getDiscountPct();
        BigDecimal discount = subtotal.multiply(discountPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discounted = subtotal.subtract(discount);
        BigDecimal taxPct = invoice.getTaxPct() == null ? BigDecimal.ZERO : invoice.getTaxPct();
        BigDecimal tax = discounted.multiply(taxPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = discounted.add(tax);
        response.send(new InvoiceSummaryView(id, subtotal, discountPct, discount, taxPct, tax, total));
    }
}

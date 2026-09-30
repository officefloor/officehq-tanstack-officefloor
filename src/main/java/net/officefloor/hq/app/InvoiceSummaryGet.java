package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/summary?invoiceId=N} — one invoice's money summary: its {@code subtotal}
 * (the sum of qty * unit price across its line items, the same total the line items add up to), the
 * {@code discountPct} recorded against it, the {@code discount} that percentage takes off the
 * subtotal, and the final {@code total} (subtotal minus the discount). Wired by
 * {@code officefloor/rest/api/invoices/summary.GET.yml}.
 *
 * <p>All three money figures are DERIVED here from the line items and the invoice's discount
 * percentage — not stored — so the invoice-detail summary panel shows the subtotal, the discount and
 * the final total without recomputing the discount in the browser.
 */
public class InvoiceSummaryGet {

    public void service(@HttpQueryParameter("invoiceId") String invoiceId, InvoiceRepository invoices,
            LineItemRepository lineItems, ObjectResponse<InvoiceSummaryView> response) {
        Long id = Long.valueOf(invoiceId.trim());

        // Subtotal: sum qty * unit price across the invoice's lines, mirroring how the line items
        // panel adds them up.
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem line : lineItems.findByInvoiceIdOrderByIdAsc(id)) {
            subtotal = subtotal.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty())));
        }

        Invoice invoice = invoices.findById(id).orElse(null);
        BigDecimal pct = invoice == null || invoice.getDiscountPct() == null
                ? BigDecimal.ZERO
                : invoice.getDiscountPct();
        BigDecimal taxPct = invoice == null || invoice.getTaxPct() == null
                ? BigDecimal.ZERO
                : invoice.getTaxPct();

        // Discount amount = subtotal * pct / 100, rounded to cents; what is left is the discounted
        // figure the tax is then worked out on.
        BigDecimal discount = subtotal.multiply(pct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discounted = subtotal.subtract(discount);

        // Sales tax is added ON TOP, AFTER the discount: tax = discounted * taxPct / 100, rounded to
        // cents; the final total is the discounted figure plus that tax.
        BigDecimal tax = discounted.multiply(taxPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = discounted.add(tax);

        response.send(new InvoiceSummaryView(subtotal, pct, discount, taxPct, tax, total));
    }
}

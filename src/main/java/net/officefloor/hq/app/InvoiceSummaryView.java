package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for an invoice's money summary: the SUBTOTAL (the sum of its line items), the
 * percentage DISCOUNT taken off it, the DISCOUNT amount that percentage works out to, and the final
 * TOTAL (subtotal minus the discount). The derivation lives on the server, computed in
 * {@link BigDecimal} so there is no float drift — the shape the invoice detail page renders into its
 * subtotal / discount / total figures.
 */
public record InvoiceSummaryView(Long invoiceId, BigDecimal subtotal, BigDecimal discountPct,
        BigDecimal discount, BigDecimal total) {
}

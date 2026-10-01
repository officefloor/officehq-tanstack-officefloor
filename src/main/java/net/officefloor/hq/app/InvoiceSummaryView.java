package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for an invoice's money summary: the SUBTOTAL (the sum of its line items), the
 * percentage DISCOUNT taken off it and the DISCOUNT amount that works out to, the percentage sales
 * TAX added on top and the TAX amount that works out to, and the final TOTAL. Tax is applied after
 * the discount, so the total is the subtotal minus the discount, plus the tax on what is left. The
 * derivation lives on the server, computed in {@link BigDecimal} so there is no float drift — the
 * shape the invoice detail page renders into its subtotal / discount / tax / total figures.
 */
public record InvoiceSummaryView(Long invoiceId, BigDecimal subtotal, BigDecimal discountPct,
        BigDecimal discount, BigDecimal taxPct, BigDecimal tax, BigDecimal total) {
}

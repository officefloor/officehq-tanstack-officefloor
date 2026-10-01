package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for one invoice's outstanding balance: how much is still left to pay after
 * any payments. The amount due keeps its scale (a {@link BigDecimal}) so the client formats money to
 * two places. Derived (invoice amount minus the sum of its payments), never stored.
 */
public record InvoiceDueView(Long invoiceId, BigDecimal amountDue) {
}

package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * One invoice as it appears on a client's statement: the invoice, the project it is for, its stage,
 * its amount, and how much is still DUE on it (the invoice amount minus the sum of its payments).
 * The amounts keep their {@link BigDecimal} scale so the client formats money to two places. The
 * due figure is derived (never stored), the same derivation {@link InvoiceAmountDue} applies to a
 * single invoice.
 */
public record StatementInvoiceView(Long id, Long projectId, String status, BigDecimal amount,
        BigDecimal amountDue) {
}

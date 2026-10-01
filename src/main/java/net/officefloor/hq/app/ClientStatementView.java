package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * A client's statement: all of the client's invoices in one place, and the TOTAL they still owe
 * (the sum of what is due across those invoices). The join across projects lives on the server —
 * the client owns projects, a project owns invoices — so the one view carries every invoice without
 * the client stitching it together. The outstanding total keeps its {@link BigDecimal} scale so the
 * client formats it to two places. The same invoices are also grouped by job ({@code projects}, in
 * id order), each group carrying its own subtotal, so the client can show the statement grouped by
 * job without stitching the groups together — the group subtotals sum to the outstanding total.
 */
public record ClientStatementView(Long clientId, List<StatementInvoiceView> invoices,
        List<StatementProjectView> projects, BigDecimal outstandingTotal) {
}

package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * One project (job) as it appears on a client's statement: the project, its name, the invoices
 * raised against it (id order), and the SUBTOTAL still owed across those invoices (the sum of their
 * dues). Grouping the statement by job lives on the server alongside the cross-project join, so the
 * client renders the groups without stitching them together. The subtotal keeps its
 * {@link BigDecimal} scale so the client formats money to two places, and the subtotals sum to the
 * statement's outstanding total.
 */
public record StatementProjectView(Long projectId, String name, BigDecimal subtotal,
        List<StatementInvoiceView> invoices) {
}

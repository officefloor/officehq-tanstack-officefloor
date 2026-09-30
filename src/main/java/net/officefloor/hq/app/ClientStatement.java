package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * A client's statement: every invoice raised for the client (across all their projects), each
 * carrying how much is still due (amount - payments, via {@link ProjectInvoice}), plus the single
 * headline figure the client still owes — {@code totalOwed}, the sum of those dues. Derived per
 * request in {@link ClientStatementGet} so it always reflects the current invoices and payments.
 */
public class ClientStatement {

    private final List<ProjectInvoice> invoices;
    private final BigDecimal totalOwed;

    public ClientStatement(List<ProjectInvoice> invoices, BigDecimal totalOwed) {
        this.invoices = invoices;
        this.totalOwed = totalOwed;
    }

    public List<ProjectInvoice> getInvoices() {
        return invoices;
    }

    public BigDecimal getTotalOwed() {
        return totalOwed;
    }
}

package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * What {@code GET /api/clients/statement} returns: a client's statement — all of that client's
 * invoices in one place (each carrying its derived {@code amountDue}, its amount minus every
 * payment recorded against it) and the {@code outstanding} total the client still owes (the sum of
 * those amounts due). A read-only aggregate joined in {@link ClientStatementGet}, mirroring how
 * {@link DashboardView} carries a server-derived outstanding total; no entity of its own.
 */
public class StatementView {

    private final List<Invoice> invoices;
    private final BigDecimal outstanding;

    public StatementView(List<Invoice> invoices, BigDecimal outstanding) {
        this.invoices = invoices;
        this.outstanding = outstanding;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }
}

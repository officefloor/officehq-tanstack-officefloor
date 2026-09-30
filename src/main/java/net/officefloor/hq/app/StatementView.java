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
    private final String currency;

    public StatementView(List<Invoice> invoices, BigDecimal outstanding, String currency) {
        this.invoices = invoices;
        this.outstanding = outstanding;
        this.currency = currency;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    /** The currency this client's statement is shown in (their billing currency). */
    public String getCurrency() {
        return currency;
    }
}

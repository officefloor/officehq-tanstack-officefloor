package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * A client's statement: every invoice raised for the client (across all their projects), each
 * carrying how much is still due (amount - payments, via {@link ProjectInvoice}), plus the single
 * headline figure the client still owes — {@code totalOwed}, the sum of those dues. Derived per
 * request in {@link ClientStatementGet} so it always reflects the current invoices and payments.
 *
 * The same invoices are also grouped by job under {@code projects} — one {@link StatementProject}
 * per project, each carrying that job's own subtotal — so the statement can present the invoices
 * under their job with a per-job subtotal. The flat {@code invoices} list is kept for readers that
 * want every row regardless of job; {@code totalOwed} is unchanged (it still sums every due).
 */
public class ClientStatement {

    private final List<StatementProject> projects;
    private final List<ProjectInvoice> invoices;
    private final BigDecimal totalOwed;

    public ClientStatement(List<StatementProject> projects, List<ProjectInvoice> invoices,
            BigDecimal totalOwed) {
        this.projects = projects;
        this.invoices = invoices;
        this.totalOwed = totalOwed;
    }

    public List<StatementProject> getProjects() {
        return projects;
    }

    public List<ProjectInvoice> getInvoices() {
        return invoices;
    }

    public BigDecimal getTotalOwed() {
        return totalOwed;
    }
}

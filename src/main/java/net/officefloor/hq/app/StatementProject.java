package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * One job's slice of a client's statement: the project (job) the invoices were raised against, the
 * invoices themselves ({@link ProjectInvoice}, each with its own money still due), and the
 * {@code subtotal} owed for the job — the sum of those dues. Built per request in
 * {@link ClientStatementGet} so it always reflects the current invoices and payments; the statement
 * groups its invoices under one of these per job and shows the subtotal on each.
 */
public class StatementProject {

    private final Long projectId;
    private final String projectName;
    private final List<ProjectInvoice> invoices;
    private final BigDecimal subtotal;

    public StatementProject(Long projectId, String projectName, List<ProjectInvoice> invoices) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.invoices = invoices;
        BigDecimal total = BigDecimal.ZERO;
        for (ProjectInvoice invoice : invoices) {
            total = total.add(invoice.getDue());
        }
        this.subtotal = total;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<ProjectInvoice> getInvoices() {
        return invoices;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}

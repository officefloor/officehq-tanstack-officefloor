package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What {@code GET /api/dashboard} returns: the whole-app summary the home screen shows — how many
 * clients and projects exist, and how much money is still owed (the sum of every SENT invoice's
 * amount — drafts and paid invoices excluded). A read-only aggregate joined in {@link DashboardGet};
 * no entity of its own.
 */
public class DashboardView {

    private final long clientsCount;
    private final long projectsCount;
    private final BigDecimal outstandingTotal;
    private final long overdueCount;

    public DashboardView(long clientsCount, long projectsCount, BigDecimal outstandingTotal,
            long overdueCount) {
        this.clientsCount = clientsCount;
        this.projectsCount = projectsCount;
        this.outstandingTotal = outstandingTotal;
        this.overdueCount = overdueCount;
    }

    public long getClientsCount() {
        return clientsCount;
    }

    public long getProjectsCount() {
        return projectsCount;
    }

    public BigDecimal getOutstandingTotal() {
        return outstandingTotal;
    }

    /** How many SENT invoices are overdue against the dashboard's reference date. */
    public long getOverdueCount() {
        return overdueCount;
    }
}

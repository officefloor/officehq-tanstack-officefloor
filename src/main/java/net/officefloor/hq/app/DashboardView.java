package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What {@code GET /api/dashboard} returns: the whole-app summary the home screen shows — how many
 * clients and projects exist, and how much money is still owed (the sum of every UNPAID invoice's
 * amount). A read-only aggregate joined in {@link DashboardGet}; no entity of its own.
 */
public class DashboardView {

    private final long clientsCount;
    private final long projectsCount;
    private final BigDecimal outstandingTotal;

    public DashboardView(long clientsCount, long projectsCount, BigDecimal outstandingTotal) {
        this.clientsCount = clientsCount;
        this.projectsCount = projectsCount;
        this.outstandingTotal = outstandingTotal;
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
}

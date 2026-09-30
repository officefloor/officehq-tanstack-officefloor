package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What {@code GET /api/dashboard} returns: the whole-app summary the home screen shows — how many
 * clients and projects exist, and how much money is still owed (the sum of every SENT invoice's
 * amount — drafts and paid invoices excluded), kept SEPARATE per currency since clients are billed in
 * different currencies and two currencies are never added together. A read-only aggregate joined in
 * {@link DashboardGet}; no entity of its own.
 */
public class DashboardView {

    private final long clientsCount;
    private final long projectsCount;
    private final Map<String, BigDecimal> outstandingByCurrency;
    private final long overdueCount;

    public DashboardView(long clientsCount, long projectsCount,
            Map<String, BigDecimal> outstandingByCurrency, long overdueCount) {
        this.clientsCount = clientsCount;
        this.projectsCount = projectsCount;
        this.outstandingByCurrency = outstandingByCurrency;
        this.overdueCount = overdueCount;
    }

    public long getClientsCount() {
        return clientsCount;
    }

    public long getProjectsCount() {
        return projectsCount;
    }

    /**
     * How much is still owed, keyed by currency code (e.g. {@code {"USD": 100.00, "EUR": 200.00}}) —
     * one entry per currency any SENT invoice is billed in, never summed across currencies.
     */
    public Map<String, BigDecimal> getOutstandingByCurrency() {
        return outstandingByCurrency;
    }

    /** How many SENT invoices are overdue against the dashboard's reference date. */
    public long getOverdueCount() {
        return overdueCount;
    }
}

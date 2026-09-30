package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The home dashboard's headline figures: how many clients and projects the user has, and how much
 * money is still owed (the sum of all UNPAID invoice amounts). A read-only cross-entity aggregate,
 * built by {@link DashboardSummaryGet} and serialised as the JSON the dashboard reads.
 */
public class DashboardSummary {

    private final long clients;
    private final long projects;
    private final BigDecimal outstanding;
    private final Map<String, BigDecimal> outstandingByCurrency;
    private final long overdue;

    public DashboardSummary(long clients, long projects, BigDecimal outstanding,
            Map<String, BigDecimal> outstandingByCurrency, long overdue) {
        this.clients = clients;
        this.projects = projects;
        this.outstanding = outstanding;
        this.outstandingByCurrency = outstandingByCurrency;
        this.overdue = overdue;
    }

    public long getClients() {
        return clients;
    }

    public long getProjects() {
        return projects;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    /**
     * How much is still owed, split out per currency (keyed by ISO code, e.g. USD/EUR). The totals are
     * kept separate — money in different currencies is never added together.
     */
    public Map<String, BigDecimal> getOutstandingByCurrency() {
        return outstandingByCurrency;
    }

    /** How many SENT invoices are past their due date as of the dashboard's reference date. */
    public long getOverdue() {
        return overdue;
    }
}

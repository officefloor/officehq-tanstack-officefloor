package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The home dashboard's headline figures: how many clients and projects the user has, and how much
 * money is still owed (the sum of all UNPAID invoice amounts). A read-only cross-entity aggregate,
 * built by {@link DashboardSummaryGet} and serialised as the JSON the dashboard reads.
 */
public class DashboardSummary {

    private final long clients;
    private final long projects;
    private final BigDecimal outstanding;

    public DashboardSummary(long clients, long projects, BigDecimal outstanding) {
        this.clients = clients;
        this.projects = projects;
        this.outstanding = outstanding;
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
}

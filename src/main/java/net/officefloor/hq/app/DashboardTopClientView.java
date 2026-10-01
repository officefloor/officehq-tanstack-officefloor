package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * One row of the home dashboard's "top clients" panel: a client and how much that client still owes
 * (the sum of what is still due across every invoice the client's projects hold — the same figure
 * the client statement totals). The shape the dashboard renders into each ranked row.
 */
public record DashboardTopClientView(Long clientId, String name, BigDecimal amount) {
}

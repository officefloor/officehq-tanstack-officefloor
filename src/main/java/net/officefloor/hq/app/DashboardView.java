package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The home dashboard summary the API exposes: how many clients and projects exist, the total still
 * owed (the sum of SENT invoice amounts across all projects), and how many SENT invoices are
 * overdue (past their due date as of the dashboard's reference date). The shape the home page
 * renders into its tiles.
 */
public record DashboardView(long clients, long projects, BigDecimal outstanding, long overdue) {
}

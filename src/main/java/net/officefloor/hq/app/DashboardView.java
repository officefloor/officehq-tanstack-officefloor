package net.officefloor.hq.app;

import java.util.List;

/**
 * The home dashboard summary the API exposes: how many clients and projects exist, what is still
 * owed kept SEPARATE per currency (clients are billed in different currencies and their money is
 * never added together), and how many SENT invoices are overdue (past their due date as of the
 * dashboard's reference date). The shape the home page renders into its tiles.
 */
public record DashboardView(long clients, long projects,
        List<CurrencyAmount> outstandingByCurrency, long overdue) {
}

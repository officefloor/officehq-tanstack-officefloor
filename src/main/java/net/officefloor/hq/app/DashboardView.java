package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The home dashboard summary the API exposes: how many clients and projects exist, and the total
 * still owed (the sum of UNPAID invoice amounts across all projects). The shape the home page
 * renders into its three tiles.
 */
public record DashboardView(long clients, long projects, BigDecimal outstanding) {
}

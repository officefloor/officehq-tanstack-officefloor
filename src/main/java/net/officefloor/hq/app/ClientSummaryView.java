package net.officefloor.hq.app;

/**
 * At-a-glance counts for one client: how many projects it owns and how many contacts it keeps. The
 * shape the client detail page renders into its two count badges (client-projects-count /
 * client-contacts-count). Computed server-side so the page holds no arithmetic — it just renders.
 */
public record ClientSummaryView(long projects, long contacts) {
}

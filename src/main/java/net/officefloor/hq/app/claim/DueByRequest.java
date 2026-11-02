package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/claims/{claim}/due-by: the claim's new due-by (yyyy-MM-dd). */
public record DueByRequest(String dueBy) {
}

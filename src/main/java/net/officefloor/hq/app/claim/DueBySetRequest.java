package net.officefloor.hq.app.claim;

/** Body of setting a claim's due-by: who is setting it and the new due-by (yyyy-mm-dd). */
public record DueBySetRequest(String user, String dueBy) {
}

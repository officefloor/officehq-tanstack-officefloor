package net.officefloor.hq.app.claim;

/** Body of setting a claim's due-by directly: the new due-by (yyyy-mm-dd), and who sets it. */
public record ClaimDueBySetRequest(String dueBy, String user) {
}

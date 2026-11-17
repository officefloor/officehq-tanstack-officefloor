package net.officefloor.hq.app.claim;

/** Body of setting a claim's due-by: the new due-by (yyyy-MM-dd), and who sets it. */
public record DueByRequest(String dueBy, String by) {
}

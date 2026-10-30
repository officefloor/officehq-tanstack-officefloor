package net.officefloor.hq.app.claim;

/** The body of a due-by set: {@code dueBy} is the claim's new due-by (yyyy-MM-dd). */
public record DueByRequest(String dueBy) {
}

package net.officefloor.hq.app.claim;

/** What setting a claim's due-by submits: its new due-by (yyyy-MM-dd; null or blank: none typed). */
public record DueByRequest(String dueBy) {
}

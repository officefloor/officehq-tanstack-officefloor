package net.officefloor.hq.app.claim;

/** A claim's re-dating: the date it had and the one it has now. */
public record Redating(String oldDate, String newDate) {
}

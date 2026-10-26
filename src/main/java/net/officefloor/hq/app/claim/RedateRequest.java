package net.officefloor.hq.app.claim;

/** The body of a re-date: {@code date} is the claim's new date (yyyy-MM-dd). */
public record RedateRequest(String date) {
}

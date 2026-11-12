package net.officefloor.hq.app.claim;

/** Body of a claim's re-dating, as sent: its new date (yyyy-MM-dd) and who re-dates it. */
public record RedateRequest(String date, String user) {
}

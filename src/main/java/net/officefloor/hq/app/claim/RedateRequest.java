package net.officefloor.hq.app.claim;

/** Body of re-dating a claim: its new date (yyyy-MM-dd), and who re-dates it. */
public record RedateRequest(String date, String by) {
}

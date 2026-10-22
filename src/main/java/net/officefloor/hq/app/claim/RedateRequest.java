package net.officefloor.hq.app.claim;

/** Body of a re-date: who is making it and the claim's new date (yyyy-mm-dd). */
public record RedateRequest(String user, String date) {
}

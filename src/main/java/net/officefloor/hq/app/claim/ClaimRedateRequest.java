package net.officefloor.hq.app.claim;

/** Body of a re-dating: the claim's new date (yyyy-mm-dd), and who re-dates it. */
public record ClaimRedateRequest(String date, String user) {
}

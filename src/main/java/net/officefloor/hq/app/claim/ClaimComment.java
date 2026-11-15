package net.officefloor.hq.app.claim;

/** A comment on a claim: its text (trimmed, not empty), the user who wrote it and the sheet's today
 *  (yyyy-MM-dd) when it was written. */
public record ClaimComment(String by, String date, String text) {

    /** How it is shown on the claim: {@code <user> on <date>: <text>}. */
    public String line() {
        return by + " on " + date + ": " + text;
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A comment on a claim: the user who wrote it, the sheet's today when it was written, and its text. */
public record ClaimComment(String user, LocalDate date, String text) {

    /** The comment as a claim shows it: "<user> on <date>: <text>". */
    public String line() {
        return user + " on " + date + ": " + text;
    }
}

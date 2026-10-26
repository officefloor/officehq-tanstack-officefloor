package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A comment written on a claim: its text (trimmed), by the user, on the sheet's today then. */
public record ClaimComment(String user, LocalDate on, String text) {
}

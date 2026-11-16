package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A comment on a claim: who wrote it, the sheet's today when it was written (null until stored) and its text. */
public record ClaimComment(String by, LocalDate date, String text) {
}

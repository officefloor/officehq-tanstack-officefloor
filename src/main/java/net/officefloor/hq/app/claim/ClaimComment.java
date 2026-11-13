package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A comment on a claim: the user who wrote it, the sheet's today then and its text. */
public record ClaimComment(long claimId, String by, LocalDate date, String text) {
}

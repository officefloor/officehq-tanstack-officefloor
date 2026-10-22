package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim add or edit. The date is null when the sheet's today is to be used; corrects is the
 *  number of the claim a correction (a negative amount) corrects, null for any other claim; ref is its external
 *  reference, trimmed (null: none); tags are its tags, normalised and joined by commas ("": none; null: not
 *  given, so an edit keeps the ones it has). */
public record ClaimInput(String user, BigDecimal amount, LocalDate date, String note, String corrects, String ref, String tags) {
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim form: the amount, the date (null for the sheet's today), the trimmed note and the number of
 * the claim it corrects (null for none; given exactly when the amount is negative) and the trimmed external reference (null for none) and the tags, normalised as Tags.normalise does (null for none). */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref, String tags) {
}

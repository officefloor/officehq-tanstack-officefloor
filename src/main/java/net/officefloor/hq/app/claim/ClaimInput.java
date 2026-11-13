package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim request; {@code date} is null when it was left blank (the sheet's entry date, else its today); {@code corrects}
 * is the number of the claim a correction (a negative amount) corrects, null for any other claim; {@code ref} is its external
 * reference, surrounding spaces removed, null when none (blank) was given; {@code tags} its tags, each
 * trimmed and lower-cased, repeats dropped, sorted and comma separated, null when none were given. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref, String tags) {
}

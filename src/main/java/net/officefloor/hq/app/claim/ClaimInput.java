package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim body. The date is null when none was given (it defaults to the sheet's today); corrects is null when
 *  the claim names no claim it corrects. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, Integer corrects) {
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim body. `date` is null when none was given: the sheet's today is used.
 *  `corrects` is the number of the claim a correction corrects; null for any other claim. */
public record ClaimValues(BigDecimal amount, LocalDate date, String note, Long corrects, String user) {
}

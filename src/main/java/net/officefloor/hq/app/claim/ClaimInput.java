package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A validated claim body. {@code date} is null when it was left blank: the sheet's today. {@code corrects} is the
 * number of the claim a correction (a negative amount) corrects; null for any other claim.
 */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects) {
}

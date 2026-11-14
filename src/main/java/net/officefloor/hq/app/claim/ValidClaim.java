package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim request once checked: the amount to two decimals, the date (null: the sheet's today), the note. */
public record ValidClaim(BigDecimal amount, LocalDate date, String note) {
}

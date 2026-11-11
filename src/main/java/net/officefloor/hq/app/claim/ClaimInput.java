package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim add or edit: the date already defaulted to the sheet's today. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String user) {
}

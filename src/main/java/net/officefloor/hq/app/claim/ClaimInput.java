package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim add or edit. The date is null when the sheet's today is to be used. */
public record ClaimInput(String user, BigDecimal amount, LocalDate date, String note) {
}

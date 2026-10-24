package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim form: the amount, the date (null for the sheet's today) and the trimmed note. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note) {
}

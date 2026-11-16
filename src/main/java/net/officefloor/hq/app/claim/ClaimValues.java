package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim's values once validated. {@code date} is null when none was given (the sheet's today applies). */
public record ClaimValues(BigDecimal amount, LocalDate date, String note) {
}

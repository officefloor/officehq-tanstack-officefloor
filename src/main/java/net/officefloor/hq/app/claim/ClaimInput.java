package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim's fields; a null date means the sheet's today. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note) {
}

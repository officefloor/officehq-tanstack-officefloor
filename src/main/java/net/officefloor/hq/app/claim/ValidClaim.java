package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim form that passed validation. A null date takes the sheet's today. */
public record ValidClaim(BigDecimal amount, LocalDate date, String note, String user) {
}

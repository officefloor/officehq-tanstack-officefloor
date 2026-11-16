package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim of a person on a sheet: an amount (0.00 or more, two decimals), a date and a note (may be empty). */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note) {
}

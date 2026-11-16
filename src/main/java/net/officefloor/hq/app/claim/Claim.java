package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim of a person on a sheet: an amount (0.00 or more, two decimals), a date, a note (may be empty), its status
 * and its number (given when opened; null for a draft or a voided claim).
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Long number) {
}

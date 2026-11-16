package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim of a person on a sheet: an amount (0.00 or more, two decimals), a date, a note (may be empty), its status
 * its number (given when opened; kept when voided; null for a draft) and its due fixed when settled (null when not
 * settled, or settled while its grade had no rate), who opened it (null when never opened),
 * whether it is bound to the figure it had at its person's last statement ({@code stated}) and that figure
 * ({@code statedDue}; null when it had none).
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Long number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue) {
}

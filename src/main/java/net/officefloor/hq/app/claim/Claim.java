package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim of a person on a sheet: an amount (two decimals; negative only for a correction), a date, a note (may be empty), its status
 * its number (given when opened; kept when voided; null for a draft) and its due fixed when settled (null when not
 * settled, or settled while its grade had no rate), who opened it (null when never opened),
 * whether it is bound to the figure it had at its person's last statement ({@code stated}) and that figure
 * ({@code statedDue}; null when it had none), and for a correction (a negative amount) the number of the SETTLED
 * claim of the same person it corrects ({@code corrects}; null for any other claim), and its due-by ({@code dueBy}), fixed
 * when it is added from its person's own terms, else those of their grade in force on its date (see SettingRepository#terms), and the
 * date it is deferred until ({@code deferredUntil}; null when never deferred; see ClaimDue#deferred), and its proofs
 * count ({@code proofs}; 0 when added, raised by one at a time; see ApplyProof), and for a claim split off another
 * the number of the claim it was split from ({@code splitFrom}; null for any other claim; see ApplySplit).
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
        String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs,
        String splitFrom) {
}

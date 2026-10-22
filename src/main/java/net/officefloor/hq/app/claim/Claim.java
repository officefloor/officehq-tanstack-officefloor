package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim: an amount (two decimals; below 0.00 only for a correction), a date, a note (may be empty), its
 *  status, its number (sheet-wide digits, or <period name>-<n> / X-<n> when numbered within its period; null while a draft; a voided claim keeps the one it had) and, once settled, the due fixed
 *  at settling (null before then, or when the person's grade had no rate then: its due stays live), and
 *  who opened it (null while a draft), whether its person's last statement bound it (it counted then) and the
 *  figure it bound it to (null when not bound, or its figure was n/a then: its due stays live), and for a correction
 *  (a negative amount) the number of the SETTLED claim of the same person it corrects (null otherwise). */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue, String corrects) {
}

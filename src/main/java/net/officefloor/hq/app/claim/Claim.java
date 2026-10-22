package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim: an amount (two decimals; below 0.00 only for a correction), a date, a note (may be empty), its
 *  status, its number (sheet-wide digits, or <period name>-<n> / X-<n> when numbered within its period; null while a draft; a voided claim keeps the one it had) and, once settled, the due fixed
 *  at settling (null before then, or when the person's grade had no rate then: its due stays live), and
 *  who opened it (null while a draft), whether its person's last statement bound it (it counted then) and the
 *  figure it bound it to (null when not bound, or its figure was n/a then: its due stays live), and for a correction
 *  (a negative amount) the number of the SETTLED claim of the same person it corrects (null otherwise), and its
 *  due-by, fixed when it was created: its date plus the terms of its person's grade in force on its date (else the
 *  sheet's terms); it does not change when the terms or its date change later; and the date it is deferred until (null: never
 *  deferred): an OPEN claim deferred past the sheet's today is deferred (see deferredOn); and its proofs count (0 or more,
 *  raised one at a time); and the number of the claim it was split from (null when it was not split off one). */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue, String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs,
        String splitFrom) {

    /** Whether the claim is deferred on the sheet's today: it is OPEN and deferred until a date after today (null:
     *  no today, nothing is deferred). Deferred, it counts in no total, is not overdue, cannot be settled and is
     *  left unbound by a statement; from that date on it is an OPEN claim like any other. */
    public boolean deferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today != null && deferredUntil.isAfter(today);
    }
}

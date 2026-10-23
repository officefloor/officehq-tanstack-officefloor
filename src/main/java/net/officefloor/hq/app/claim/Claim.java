package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim: an amount (two decimals; below 0.00 only for a correction), a date, a note (may be empty), its
 *  status, its number (sheet-wide digits as given before fiscal-year numbering, FY<yyyy>-<n> within the tree's fiscal year, or <period name>-<n> / X-<n> when numbered within its period; null while a draft; a voided claim keeps the one it had) and, once settled, the due fixed
 *  at settling (null before then, or when the person's grade had no rate then: its due stays live), and
 *  who opened it (null while a draft), whether its person's last statement bound it (it counted then) and the
 *  due as shown then it bound it to, late charge included (null when not bound, or it was n/a then), and for a correction
 *  (a negative amount) the number of the SETTLED claim of the same person it corrects (null otherwise), and its
 *  due-by: the terms-th business day after the date it had when it was created, with the terms fixed then (its person's
 *  own, else their grade's in force on that date, else the sheet's), skipping Saturdays, Sundays and the sheet's
 *  holidays as they are now; it does not change when the terms or its date change later; and the date it is deferred until (null: never
 *  deferred): an OPEN claim deferred past the sheet's today is deferred (see deferredOn); and its proofs count (0 or more,
 *  raised one at a time); and the number of the claim it was split from (null when it was not split off one); and whether it is a
 *  late entry: added while its period was soft-closed (fixed when it was added); and the last sheet-today a reminder was
 *  written for it (null: never reminded); and its external reference (null: none), unique on the sheet without regard to case;
 *  and its tags (null: none): trimmed, lower-cased, without repeats, sorted and joined by commas; and whether a steward waived its late charge
 *  (it then bears none, overdue or not); and how many of its proofs are photos (the rest are receipts). */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue, String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs,
        String splitFrom, boolean lateEntry, LocalDate reminded, String ref,
        String tags, boolean waived, int photos) {

    /** The proof kinds a proof can be: a receipt (when none is chosen) or a photo. */
    public static final java.util.List<String> PROOF_KINDS = java.util.List.of("receipt", "photo");

    /** How many of its proofs are of the kind: photos, or receipts (the rest). */
    public int proofsOf(String kind) {
        return "photo".equals(kind) ? photos : "receipt".equals(kind) ? proofs - photos : 0;
    }

    /** Whether the claim is deferred on the sheet's today: it is OPEN and deferred until a date after today (null:
     *  no today, nothing is deferred). Deferred, it counts in no total, is not overdue, cannot be settled and is
     *  left unbound by a statement; from that date on it is an OPEN claim like any other. */
    public boolean deferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today != null && deferredUntil.isAfter(today);
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim made by a person on a sheet. {@code amount} is 0.00 or more, in two decimals. {@code number}
 * is given when the claim is opened (null for a draft or a voided claim). {@code settledDue} is the due fixed
 * when it was settled (null when not settled, or settled while no rate of its grade applied on its date).
 * {@code openedBy} is the user who opened it (null when never opened). {@code stated} is whether its person's
 * last statement bound it, at {@code statedDue} (null when it had no figure then). A claim with a negative amount is a
 * correction: {@code corrects} is the number of the SETTLED claim of the same person it corrects (null for any other claim).
 * {@code dueBy} is fixed when the claim is made, and kept when its terms or its date change later.
 * {@code deferredUntil} is the date an OPEN claim was deferred to (null when never deferred).
 * {@code proofs} is how many proofs it has been given (0 for none).
 * {@code splitFrom} is the number of the claim it was split off (null for any other claim).
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue, String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs,
        String splitFrom) {

    public Claim with(ClaimInput input, LocalDate date) {
        return new Claim(id, sheetId, personId, input.amount(), date, input.note(), status, number, settledDue, openedBy, stated, statedDue, input.corrects(), dueBy, deferredUntil, proofs, splitFrom);
    }

    public Claim withStatus(ClaimStatus status, String number, BigDecimal settledDue, String openedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, splitFrom);
    }

    /** The claim moved to another person: unbound from any statement, so live until their next one. */
    public Claim withPerson(long personId) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null, corrects, dueBy, deferredUntil, proofs, splitFrom);
    }

    /** The claim re-dated to {@code date}, numbered {@code number}. */
    public Claim withDate(LocalDate date, String number) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, splitFrom);
    }

    /** The claim deferred to {@code until}. */
    public Claim withDeferredUntil(LocalDate until) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, until, proofs, splitFrom);
    }

    /** The claim with {@code amount} as its amount, unbound from any statement (live until its person's next one). */
    public Claim withAmountUnbound(BigDecimal amount) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null, corrects, dueBy, deferredUntil, proofs, splitFrom);
    }

    /** The claim with {@code proofs} proofs. */
    public Claim withProofs(int proofs) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, splitFrom);
    }

    /**
     * Whether the claim is deferred on {@code today}: an OPEN claim deferred to a date the sheet's today has not reached
     * counts in no total, is not overdue, cannot be settled and is left unbound by a statement.
     */
    public boolean deferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && (today == null || today.isBefore(deferredUntil));
    }

    /** A correction: its due is its amount times the rate, with no floor and no late charge. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;

/** The figures a claim has: live, fixed when settled, or bound by a statement. */
public final class ClaimDue {

    private ClaimDue() {
    }

    /** Whether a claim counts toward claimed and the totals: only OPEN, PENDING and SETTLED claims do. */
    public static boolean counts(Claim claim) {
        return isOpen(claim) || claim.status() == ClaimStatus.SETTLED;
    }

    /** Whether a claim is open: OPEN, or PENDING (handled like an OPEN claim but for its statement binding). */
    public static boolean isOpen(Claim claim) {
        return claim.status() == ClaimStatus.OPEN || claim.status() == ClaimStatus.PENDING;
    }

    /** Whether a claim counts toward the live totals (claimed, due-totals, a period's due-total): one that counts,
     *  unless it is open (OPEN or PENDING), not bound by a statement, and dated within a hold of its person (null: none). */
    public static boolean countsLive(Claim claim, net.officefloor.hq.app.person.Person person) {
        return counts(claim) && !(isOpen(claim) && !bound(claim) && person != null && person.heldOn(claim.date()));
    }

    /** Whether a claim is deferred on the given today: an OPEN claim deferred until a date after it. Until then it
     *  counts in no total, is not overdue, cannot be settled and is left unbound by a statement; from that day on
     *  it is an OPEN claim like any other. */
    public static boolean deferred(Claim claim, java.time.LocalDate today) {
        return claim.status() == ClaimStatus.OPEN && claim.deferredUntil() != null
                && claim.deferredUntil().isAfter(today);
    }

    /** Whether a claim counts toward the live totals on the given today: as {@link #countsLive(Claim,
     *  net.officefloor.hq.app.person.Person)}, unless it is deferred. */
    public static boolean countsLive(Claim claim, net.officefloor.hq.app.person.Person person,
            java.time.LocalDate today) {
        return countsLive(claim, person) && !deferred(claim, today);
    }

    /** The claim's rate figure: its amount at the grade's rate in force on its date, rounded to two decimals by the
     *  claim's rounding (see Rounding); null when no rate applies. The floor is applied last, after the late charge
     *  (see LateCharge#add). */
    public static BigDecimal live(Claim claim, String grade, RateRepository rates, Rounding rounding) {
        Rate rate = rates.find(claim.sheetId(), grade, claim.date());
        if (rate == null) {
            return null;
        }
        return claim.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, rounding.mode(claim));
    }

    /** Whether the claim is a correction: its amount is negative. */
    public static boolean isCorrection(Claim claim) {
        return claim.amount().signum() < 0;
    }

    /** The figure the claim shows: fixed when settled, else the one a statement bound it to (never while PENDING),
     *  else its live rate figure; null when it has none (n/a). */
    public static BigDecimal figure(Claim claim, String grade, RateRepository rates, Rounding rounding) {
        if (claim.due() != null) {
            return claim.due();
        }
        return bound(claim) ? claim.statedDue() : live(claim, grade, rates, rounding);
    }

    /** Whether the claim keeps a figure whatever the rates are: fixed when settled, or bound by a statement. */
    public static boolean kept(Claim claim) {
        return claim.due() != null || (bound(claim) && claim.statedDue() != null);
    }

    /** Whether a statement binds the claim now: it is stated and not PENDING. */
    public static boolean bound(Claim claim) {
        return claim.stated() && claim.status() != ClaimStatus.PENDING;
    }

    /** The status a claim had at the end of a day, as its status log (oldest first) records it: that of the last line
     *  made on or before the day; null when the claim was added after it. */
    public static ClaimStatus statusAsOf(java.util.List<ClaimLogLine> log, java.time.LocalDate day) {
        ClaimStatus status = null;
        for (ClaimLogLine line : log) {
            if (!line.date().isAfter(day)) {
                status = line.status();
            }
        }
        return status;
    }

    /** The claim as it would be with another status, all else kept. */
    public static Claim withStatus(Claim c, ClaimStatus status) {
        return new Claim(c.id(), c.sheetId(), c.personId(), c.amount(), c.date(), c.note(), status, c.number(), c.due(),
                c.openedBy(), c.stated(), c.statedDue(), c.corrects(), c.dueBy(), c.deferredUntil(), c.proofs(), c.splitFrom());
    }
}

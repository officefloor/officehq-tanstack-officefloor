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

    /** Whether a claim is open: OPEN, or PENDING (handled like an OPEN claim). */
    public static boolean isOpen(Claim claim) {
        return claim.status() == ClaimStatus.OPEN || claim.status() == ClaimStatus.PENDING;
    }

    /** Whether a claim counts toward the live totals (claimed, due-totals, a period's due-total): one that counts. A
     *  hold (the claim's or its person's) does not take it out; a hold only takes a person's cut in the pools. */
    public static boolean countsLive(Claim claim, net.officefloor.hq.app.person.Person person) {
        return counts(claim);
    }

    /** Whether a claim is deferred on the given today: a claim that counts, deferred until a date after it, and not
     *  bound by a statement. Until then it counts in no total, is not overdue, cannot be settled and is left unbound
     *  by a statement; from that day on it is a claim like any other. A claim deferred while a statement binds it
     *  stays in the totals, counts and can be settled while bound: the deferral takes effect only once the binding
     *  ends (the statement is cancelled, or the next statement leaves it unbound). */
    public static boolean deferred(Claim claim, java.time.LocalDate today) {
        return counts(claim) && !bound(claim) && claim.deferredUntil() != null
                && claim.deferredUntil().isAfter(today);
    }

    /** Whether a claim counts toward the live totals on the given today: as {@link #countsLive(Claim,
     *  net.officefloor.hq.app.person.Person)}, unless it is deferred, or it is open, not bound by a statement, and its
     *  person's retirement date is reached on today. */
    public static boolean countsLive(Claim claim, net.officefloor.hq.app.person.Person person,
            java.time.LocalDate today) {
        return countsLive(claim, person) && !deferred(claim, today) && !retiredOut(claim, person, today);
    }

    /** Whether a claim counts toward claimed (a person's claimed, the claimed-total) on the given today: as {@link
     *  #countsLive(Claim, net.officefloor.hq.app.person.Person, java.time.LocalDate)}, which it now matches. */
    public static boolean countsClaimed(Claim claim, net.officefloor.hq.app.person.Person person,
            java.time.LocalDate today) {
        return counts(claim) && !deferred(claim, today) && !retiredOut(claim, person, today);
    }

    /** Whether a claim leaves the live totals for its person's retirement: it is open (OPEN or PENDING), not bound by
     *  a statement, and its person's retirement date is on or before today (null person: never). */
    public static boolean retiredOut(Claim claim, net.officefloor.hq.app.person.Person person,
            java.time.LocalDate today) {
        return isOpen(claim) && !bound(claim) && person != null && person.retiredOn(today);
    }

    /** The claim's rate figure: its amount at the grade's rate that applies to it on its date (a forward-only rate
     *  saved after it was added does not; see RateRepository#forClaim), rounded to two decimals by the
     *  claim's rounding (see Rounding); null when no rate applies. The floor is applied to it before the late charge
     *  (see LateCharge#add). */
    public static BigDecimal live(Claim claim, String grade, RateRepository rates, Rounding rounding) {
        Rate rate = rates.forClaim(claim.sheetId(), grade, claim.date(), rates.seqOf(claim.id()));
        if (rate == null) {
            return null;
        }
        return claim.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, rounding.mode(claim));
    }

    /** Whether the claim is a correction: its amount is negative. */
    public static boolean isCorrection(Claim claim) {
        return claim.amount().signum() < 0;
    }

    /** The figure the claim shows: fixed when settled, else the one a statement bound it to (PENDING included),
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

    /** Whether a statement binds the claim now: it is stated (PENDING included, like any claim that counts). */
    public static boolean bound(Claim claim) {
        return claim.stated();
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
                c.openedBy(), c.stated(), c.statedDue(), c.corrects(), c.dueBy(), c.deferredUntil(), c.proofs(), c.splitFrom(), c.ref(), c.tags(), c.held());
    }
}

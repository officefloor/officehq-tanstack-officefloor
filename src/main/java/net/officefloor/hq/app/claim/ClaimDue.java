package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;

/** The figures a claim has: live, fixed when settled, or bound by a statement. */
public final class ClaimDue {

    private ClaimDue() {
    }

    /** Whether a claim counts toward claimed and the totals: only OPEN and SETTLED claims do. */
    public static boolean counts(Claim claim) {
        return claim.status() == ClaimStatus.OPEN || claim.status() == ClaimStatus.SETTLED;
    }

    /** Whether a claim counts toward the live totals (claimed, due-totals, a period's due-total): one that counts,
     *  unless it is OPEN and its person is on hold. */
    public static boolean countsLive(Claim claim, boolean held) {
        return counts(claim) && !(held && claim.status() == ClaimStatus.OPEN);
    }

    /** The claim's amount at the grade's rate in force on its date, rounded to two decimals by the claim's rounding
     *  (see Rounding), and at least the sheet's floor (a correction has no floor); null when no rate applies. */
    public static BigDecimal live(Claim claim, String grade, RateRepository rates, BigDecimal floor, Rounding rounding) {
        Rate rate = rates.find(claim.sheetId(), grade, claim.date());
        if (rate == null) {
            return null;
        }
        BigDecimal due = claim.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, rounding.mode(claim));
        return isCorrection(claim) ? due : due.max(floor);
    }

    /** Whether the claim is a correction: its amount is negative. */
    public static boolean isCorrection(Claim claim) {
        return claim.amount().signum() < 0;
    }

    /** The figure the claim shows: fixed when settled, else the one a statement bound it to, else live; null when
     *  it has none (n/a). */
    public static BigDecimal figure(Claim claim, String grade, RateRepository rates, BigDecimal floor,
            Rounding rounding) {
        if (claim.due() != null) {
            return claim.due();
        }
        return claim.stated() ? claim.statedDue() : live(claim, grade, rates, floor, rounding);
    }

    /** Whether the claim keeps a figure whatever the rates are: fixed when settled, or bound by a statement. */
    public static boolean kept(Claim claim) {
        return claim.due() != null || (claim.stated() && claim.statedDue() != null);
    }
}

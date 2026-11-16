package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** The claim's amount at the grade's rate in force on its date, rounded half to even to two decimals; null
     *  when no rate applies. */
    public static BigDecimal live(Claim claim, String grade, RateRepository rates) {
        Rate rate = rates.find(claim.sheetId(), grade, claim.date());
        return rate == null ? null
                : claim.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The figure the claim shows: fixed when settled, else the one a statement bound it to, else live; null when
     *  it has none (n/a). */
    public static BigDecimal figure(Claim claim, String grade, RateRepository rates) {
        if (claim.due() != null) {
            return claim.due();
        }
        return claim.stated() ? claim.statedDue() : live(claim, grade, rates);
    }

    /** Whether the claim keeps a figure whatever the rates are: fixed when settled, or bound by a statement. */
    public static boolean kept(Claim claim) {
        return claim.due() != null || (claim.stated() && claim.statedDue() != null);
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.setting.Dated;

/**
 * What makes an overdue claim's late charge: the sheet's late percent (two decimals) by date, the one in force on the claim's date, and its today; a claim is due
 * by the due-by fixed when it was added. The charge is never bound by a statement: it follows the sheet's today.
 * It also carries the sheet's policy date, which decides how a claim's live figures round.
 */
public record Late(Dated pct, LocalDate today, LocalDate policy) {

    /**
     * How the claim's live figures round: half up when it rounds on a date (its own, or for a correction the date of
     * the claim it corrects) before the policy date, half to even on or after it.
     */
    public RoundingMode rounding(Claim claim) {
        return LocalDate.parse(claim.roundsOn()).isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** Whether the claim is overdue: OPEN or PENDING, not deferred, with its due-by (fixed when it was added) before the sheet's today. */
    public boolean overdue(Claim claim) {
        return !claim.deferred() && (claim.status() == ClaimStatus.OPEN || claim.status() == ClaimStatus.PENDING) && LocalDate.parse(claim.dueBy()).isBefore(today);
    }

    /**
     * The claim's late charge on its figure: the late percent in force on its date of the figure for every full 30 days it is overdue,
     * rounded in hundredths by the claim's {@link #rounding}; 0.00 when it is not overdue, and always for a correction.
     */
    public BigDecimal charge(Claim claim, BigDecimal figure) {
        if (figure == null || claim.correction() || !overdue(claim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long days = ChronoUnit.DAYS.between(LocalDate.parse(claim.dueBy()), today);
        return figure.multiply(pct.on(claim.date())).multiply(BigDecimal.valueOf(days / 30)).movePointLeft(2).setScale(2,
                rounding(claim));
    }
}

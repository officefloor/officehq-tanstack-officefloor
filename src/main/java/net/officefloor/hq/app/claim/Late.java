package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * What makes an overdue claim's late charge: the sheet's late percent (two decimals), its terms (days after a claim's
 * date by which it is due) and its today. The charge is never bound by a statement: it follows the sheet's today.
 */
public record Late(BigDecimal pct, int terms, LocalDate today) {

    /** Whether the claim is overdue: OPEN with its due-by (its date plus the terms) before the sheet's today. */
    public boolean overdue(Claim claim) {
        return claim.status() == ClaimStatus.OPEN && LocalDate.parse(claim.date()).plusDays(terms).isBefore(today);
    }

    /**
     * The claim's late charge on its figure: the late percent of the figure for every full 30 days it is overdue,
     * rounded half to even in hundredths; 0.00 when it is not overdue, and always for a correction.
     */
    public BigDecimal charge(Claim claim, BigDecimal figure) {
        if (figure == null || claim.correction() || !overdue(claim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long days = ChronoUnit.DAYS.between(LocalDate.parse(claim.date()).plusDays(terms), today);
        return figure.multiply(pct).multiply(BigDecimal.valueOf(days / 30)).movePointLeft(2).setScale(2,
                RoundingMode.HALF_EVEN);
    }
}

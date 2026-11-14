package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A claim's due figure. */
public final class Dues {
    private Dues() {
    }

    /** What a late charge goes by: the sheet's late percent, its terms (days) and its today; and the
     *  sheet's policy date (its today when blank), which decides how a claim's live figures round. */
    public record Late(String pct, int terms, LocalDate today, LocalDate policy) {

        /** How the live figures of a claim round by its round date ({@link Claim#getRoundDate}):
         *  half up before the policy date, half to even on or after it. */
        public RoundingMode rounding(String roundDate) {
            return LocalDate.parse(roundDate).isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
        }
    }

    /** An amount times a percent, divided by 100, rounded ({@code rounding}) to two decimals, and at
     *  least the sheet's floor (a correction, a negative amount, has no floor). */
    public static String of(String amount, String pct, String floor, RoundingMode rounding) {
        BigDecimal due = new BigDecimal(amount).multiply(new BigDecimal(pct))
                .divide(BigDecimal.valueOf(100), 2, rounding);
        return (isCorrection(amount) ? due : due.max(new BigDecimal(floor).setScale(2))).toPlainString();
    }

    /** Whether a claim's amount makes it a correction: it is negative. */
    public static boolean isCorrection(String amount) {
        return new BigDecimal(amount).signum() < 0;
    }

    /** The late charge on a claim's figure: the late percent of it for every full 30 days its OPEN
     *  claim is past its due-by (its date plus the terms) on the sheet's today, rounded by the
     *  policy date to two decimals; none (0.00) when it is not overdue, or on a correction. */
    public static BigDecimal charge(Claim claim, String figure, Late late) {
        if (figure == null || isCorrection(claim.getAmount()) || !ClaimStatus.OPEN.name().equals(claim.getStatus())) return BigDecimal.ZERO.setScale(2);
        LocalDate dueBy = LocalDate.parse(claim.getDate()).plusDays(late.terms());
        long periods = Math.max(0, ChronoUnit.DAYS.between(dueBy, late.today())) / 30;
        return new BigDecimal(figure).multiply(new BigDecimal(late.pct()))
                .multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100), 2, late.rounding(claim.getRoundDate()));
    }

    /** The due a claim shows: the one fixed when settled, else its figure (bound by a statement, else
     *  {@code live}; null: none) with its late charge added on top. */
    public static String shown(Claim claim, String live, Late late) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        String figure = claim.due(live);
        return figure == null ? null : new BigDecimal(figure).add(charge(claim, figure, late)).toPlainString();
    }
}

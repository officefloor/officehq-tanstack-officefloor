package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A claim's due figure. */
public final class Dues {
    private Dues() {
    }

    /** What a late charge goes by: the sheet's late percent, its terms (days) and its today. */
    public record Late(String pct, int terms, LocalDate today) {
    }

    /** An amount times a percent, divided by 100, rounded half to even to two decimals, and at least
     *  the sheet's floor. */
    public static String of(String amount, String pct, String floor) {
        return new BigDecimal(amount).multiply(new BigDecimal(pct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN)
                .max(new BigDecimal(floor).setScale(2)).toPlainString();
    }

    /** The late charge on a claim's figure: the late percent of it for every full 30 days its OPEN
     *  claim is past its due-by (its date plus the terms) on the sheet's today, rounded half to even
     *  to two decimals; none (0.00) when it is not overdue. */
    public static BigDecimal charge(Claim claim, String figure, Late late) {
        if (figure == null || !ClaimStatus.OPEN.name().equals(claim.getStatus())) return BigDecimal.ZERO.setScale(2);
        LocalDate dueBy = LocalDate.parse(claim.getDate()).plusDays(late.terms());
        long periods = Math.max(0, ChronoUnit.DAYS.between(dueBy, late.today())) / 30;
        return new BigDecimal(figure).multiply(new BigDecimal(late.pct()))
                .multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }

    /** The due a claim shows: the one fixed when settled, else its figure (bound by a statement, else
     *  {@code live}; null: none) with its late charge added on top. */
    public static String shown(Claim claim, String live, Late late) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        String figure = claim.due(live);
        return figure == null ? null : new BigDecimal(figure).add(charge(claim, figure, late)).toPlainString();
    }
}

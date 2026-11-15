package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.ToIntFunction;
import net.officefloor.hq.app.setting.DatedSetting;

/** A claim's due figure. */
public final class Dues {
    private Dues() {
    }

    /** What a late charge goes by: the sheet's late percent by date (a charge goes by the one in force on each 30-day boundary) and its today (a claim's due-by is its own); the
     *  sheet's policy date (its today when blank), which decides how a claim's live figures round; and
     *  a claim's grace days (those of its person's grade in force on its date, as in force on it; 0: none); and
     *  the sheet's rounding mode by date (half-up or half-even; none: the policy date decides). */
    public record Late(DatedSetting pct, LocalDate today, LocalDate policy, ToIntFunction<Claim> grace, DatedSetting mode) {

        /** The date a claim's lateness counts from: its due-by plus its grace days. It is overdue once
         *  this is before the sheet's today. */
        public LocalDate lateFrom(Claim claim) {
            return LocalDate.parse(claim.getDueBy()).plusDays(grace.applyAsInt(claim));
        }

        /** How the live figures of a claim round by its round date ({@link Claim#getRoundDate}): by the
         *  rounding mode in force on it, else half up before the policy date, half to even on or after it. */
        public RoundingMode rounding(String roundDate) {
            String m = mode == null ? null : mode.on(roundDate);
            if ("half-up".equals(m)) return RoundingMode.HALF_UP;
            if ("half-even".equals(m)) return RoundingMode.HALF_EVEN;
            return LocalDate.parse(roundDate).isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
        }
    }

    /** A claim's rate figure: an amount times a percent, divided by 100, rounded ({@code rounding}) to
     *  two decimals (no floor: that is applied by {@link #shown}, before the late charge). */
    public static String of(String amount, String pct, RoundingMode rounding) {
        return new BigDecimal(amount).multiply(new BigDecimal(pct))
                .divide(BigDecimal.valueOf(100), 2, rounding).toPlainString();
    }

    /** Whether a claim's amount makes it a correction: it is negative. */
    public static boolean isCorrection(String amount) {
        return new BigDecimal(amount).signum() < 0;
    }

    /** The late charge on a claim's figure: for each full 30 days its open (OPEN or PENDING) claim is past its
     *  due-by (fixed when it was added) plus its grace days on the sheet's today, the late percent in force on that
     *  boundary date (its due-by plus grace plus 30, 60, 90 ... days) of the figure plus the charges of
     *  the boundaries before it (compounding), each rounded by the policy date to two decimals, added up;
     *  none (0.00) when it is not overdue (a deferred claim never is), on a correction, or when it is waived. */
    public static BigDecimal charge(Claim claim, String figure, Late late) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        if (figure == null || isCorrection(claim.getAmount()) || !ClaimStatus.isOpen(claim.getStatus()) || claim.isDeferred() || claim.isWaived()) return total;
        LocalDate dueBy = late.lateFrom(claim);
        long periods = Math.max(0, ChronoUnit.DAYS.between(dueBy, late.today())) / 30;
        RoundingMode rounding = late.rounding(claim.getRoundDate());
        for (long k = 1; k <= periods; k++) {
            String pct = late.pct().on(dueBy.plusDays(30 * k).toString());
            if (pct == null) continue;
            total = total.add(new BigDecimal(figure).add(total).multiply(new BigDecimal(pct))
                    .divide(BigDecimal.valueOf(100), 2, rounding));
        }
        return total;
    }

    /** The due a claim shows: the one fixed when settled, else the one a statement bound it to (its due as
     *  shown then, floor and late charge included; null: none), else {@code live} (null: none) raised to at
     *  least {@code floor}, the sheet's floor in force on the claim's date (null: none; a correction has no
     *  floor), with the late charge on that floored figure on the sheet's today added on top. */
    public static String shown(Claim claim, String live, String floor, Late late) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        if (claim.isStated()) return claim.getStatedDue();
        if (live == null) return null;
        BigDecimal floored = new BigDecimal(live);
        if (floor != null && !isCorrection(claim.getAmount())) floored = floored.max(new BigDecimal(floor).setScale(2));
        String figure = floored.toPlainString();
        return floored.add(charge(claim, figure, late)).toPlainString();
    }
}

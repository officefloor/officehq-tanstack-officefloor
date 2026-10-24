package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The figures of one person's claims, as the screen works them out: a settled claim's frozen due, else the
 * figure its person's last statement bound it to, else its live due (amount times the rate in force on its date of
 * the grade its person had on its date, rounded by the sheet's policy, and at least the sheet's floor; none — n/a — when no rate applies then). An
 * overdue claim's due adds, on top of its figure, the late charge: the sheet's late percent of the figure for every
 * full 30 days it is overdue on the sheet's today, rounded like a due. A statement never binds the charge; settling
 * freezes it in. A correction's live due is its amount times the rate, rounded the same way, with no floor, and it
 * never takes a late charge. The rounding policy: a claim dated before the sheet's policy date (its today when blank)
 * rounds half up, one dated on or after it half to even; a correction goes by the date of the claim it corrects.
 */
public class ClaimFigures {

    private final List<Rate> rates;

    private final PersonGrades grades;

    private final BigDecimal floor;

    private final BigDecimal latePct;

    private final int terms;

    private final LocalDate today;

    private final LocalDate policyDate;

    /** @param rates the sheet's rates.
     *  @param grades the person's grades over time.
     *  @param sheet the sheet: its floor (a live due is at least it), and its late percent, terms and today (the
     *  late charge). */
    public ClaimFigures(List<Rate> rates, PersonGrades grades, Sheet sheet) {
        this.rates = rates;
        this.grades = grades;
        this.floor = sheet.getFloor();
        this.latePct = sheet.getLatePct();
        this.terms = sheet.getTerms();
        this.today = sheet.getToday();
        this.policyDate = sheet.policyDateInForce();
    }

    /** How the claim's live figures round: half up when the date it rounds by is before the policy date, else half
     *  to even. */
    public RoundingMode rounding(Claim claim) {
        return claim.getRoundsOn().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** Whether a claim is overdue: it is OPEN and its due-by (its date plus the terms) is before the sheet's today. */
    public boolean overdue(Claim claim) {
        return claim.getStatus() == ClaimStatus.OPEN && claim.getDate().plusDays(terms).isBefore(today);
    }

    /** The late charge on a figure: the late percent of it for every full 30 days the claim is overdue, rounded
     *  like its due; 0.00 when it is not overdue. */
    public BigDecimal lateCharge(Claim claim, BigDecimal figure) {
        if (figure == null || claim.isCorrection() || !overdue(claim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long periods = ChronoUnit.DAYS.between(claim.getDate().plusDays(terms), today) / 30;
        return figure.multiply(latePct).multiply(BigDecimal.valueOf(periods)).movePointLeft(2)
                .setScale(2, rounding(claim));
    }

    /** A live due: the amount times the percent, divided by 100, rounded as given, and at least the floor. */
    public static BigDecimal liveDue(BigDecimal amount, BigDecimal pct, BigDecimal floor, RoundingMode rounding) {
        BigDecimal due = amount.multiply(pct).movePointLeft(2).setScale(2, rounding);
        return floor != null && due.compareTo(floor) < 0 ? floor.setScale(2) : due;
    }

    /** Whether a claim counts toward the totals: only OPEN and SETTLED claims do. */
    public static boolean counts(Claim claim) {
        return claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
    }

    /** Whether a claim counts toward the live totals: as {@link #counts}, except a held person's OPEN claims. */
    public static boolean countsLive(Claim claim, boolean held) {
        return counts(claim) && !(held && claim.getStatus() == ClaimStatus.OPEN);
    }

    /** The percent of the grade's rate in force on a date. */
    private Optional<BigDecimal> rateOn(String grade, LocalDate on) {
        return rates.stream()
                .filter(r -> r.getGrade().equals(grade))
                .filter(r -> r.getFromDate() == null || !r.getFromDate().isAfter(on))
                .max(Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Rate::getPct);
    }

    /** The claim's due: its figure, plus the late charge while it is not settled; null when it has none (n/a). */
    public BigDecimal due(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        BigDecimal figure = figure(claim);
        return figure == null ? null : figure.add(lateCharge(claim, figure));
    }

    /** The claim's figure, without any late charge; null when it has none (n/a). */
    public BigDecimal figure(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        if (claim.isStated()) {
            return claim.getStatedDue();
        }
        return rateOn(grades.on(claim.getDate()), claim.getDate())
                .map(pct -> liveDue(claim.getAmount(), pct, claim.isCorrection() ? null : floor, rounding(claim)))
                .orElse(null);
    }

    /** The person's due-total: their counted claims' dues (with any late charge) added up, 0.00 with none; n/a when their grade
     *  in force on the day has no rate in force then, unless a counted claim keeps a figure: a fixed one (settled or bound), or a
     *  live one at another grade they had on its date. */
    public String dueTotal(List<Claim> claims, LocalDate today) {
        return dueTotal(claims, today, false);
    }

    /** The person's live due-total, as {@link #dueTotal(List, LocalDate)}; while they are held their OPEN
     *  claims do not count. */
    public String dueTotal(List<Claim> claims, LocalDate today, boolean held) {
        List<Claim> counted = claims.stream().filter(c -> countsLive(c, held)).toList();
        String grade = grades.on(today);
        boolean kept = counted.stream().anyMatch(c -> c.getDue() != null || (c.isStated() && c.getStatedDue() != null)
                || (!c.isStated() && !grade.equals(grades.on(c.getDate())) && figure(c) != null));
        if (rateOn(grade, today).isEmpty() && !kept) {
            return "n/a";
        }
        return counted.stream().map(this::due).filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2).toPlainString();
    }
}

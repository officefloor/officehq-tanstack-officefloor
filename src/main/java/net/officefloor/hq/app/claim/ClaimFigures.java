package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The figures of one person's claims, as the screen works them out: a settled claim's frozen due, else the
 * figure its person's last statement bound it to, else its live due (amount times the rate in force on its date of
 * the grade its person had on its date, rounded by the sheet's policy; none — n/a — when no rate applies then, or the one that would has ended by then). An
 * overdue claim's due adds, on top of its figure, the late charge: for each full 30 days it is overdue on the sheet's today, the
 * sheet's late percent in force on that boundary date (its due-by plus its grade's grace, plus 30, 60, 90 ... days) of the figure plus
 * the charges of the boundaries before it (compounding), rounded like a due, and
 * the charges added up (a boundary whose percent is 0.00 adds nothing). The floor comes first: an unsettled claim's due is the
 * greater of the sheet's floor in force on its date and its figure (the floored figure), plus the late charge worked out on that
 * floored figure. A statement binds the due as shown (the floored figure and its late charge), which then no longer follows the sheet's today; settling freezes the due in. A correction's live due is its amount times the rate, rounded the same way, with no floor, and it
 * never takes a late charge. The rounding policy: a claim dated before the sheet's policy date (its today when blank)
 * rounds half up, one dated on or after it half to even — unless the sheet has a rounding mode (half-up or half-even) in force on
 * that date, which it then rounds by; a correction goes by the date of the claim it corrects.
 */
public class ClaimFigures {

    private final List<Rate> rates;

    private final PersonGrades grades;

    private final Sheet sheet;

    private final LocalDate today;

    private final LocalDate policyDate;

    private final BigDecimal cap;

    /** @param rates the sheet's rates.
     *  @param grades the person's grades over time.
     *  @param sheet the sheet: its floor on a claim's date (a live due is at least it), and its late percent on each
     *  30-day boundary of a claim's due-by up to its today (the late charge), and its cap in force on its today. */
    public ClaimFigures(List<Rate> rates, PersonGrades grades, Sheet sheet) {
        this.rates = rates;
        this.grades = grades;
        this.sheet = sheet;
        this.today = sheet.getToday();
        this.policyDate = sheet.policyDateInForce();
        this.cap = sheet.getCap();
    }

    /** How the claim's live figures round: by the sheet's rounding mode in force on the date it rounds by, when one
     *  is; else half up when that date is before the policy date, else half to even. */
    public RoundingMode rounding(Claim claim) {
        String mode = sheet.roundingOn(claim.getRoundsOn());
        if (Sheet.HALF_UP.equals(mode)) {
            return RoundingMode.HALF_UP;
        }
        if (Sheet.HALF_EVEN.equals(mode)) {
            return RoundingMode.HALF_EVEN;
        }
        return claim.getRoundsOn().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** The date a claim's lateness counts from: its due-by (fixed when it was created) plus the grace days in force on
     *  its date of the grade its person had on its date (none: its due-by). */
    public LocalDate lateFrom(Claim claim) {
        Integer grace = sheet.gradeGraceOn(grades.on(claim.getDate()), claim.getDate());
        return grace == null ? claim.getDueBy() : claim.getDueBy().plusDays(grace);
    }

    /** Whether a claim is overdue: it is OPEN or PENDING, not deferred, and its due-by plus any grace of its grade
     *  ({@link #lateFrom}) is before the sheet's today. */
    public boolean overdue(Claim claim) {
        return claim.getStatus().isOpen() && !claim.isDeferredOn(today) && lateFrom(claim).isBefore(today);
    }

    /** The late charge on a figure: for each full 30 days the claim is overdue, the late percent in force on that
     *  boundary date of it plus the charges before it, rounded like its due, added up; 0.00 when it is not overdue,
     *  or its late charge is waived. */
    public BigDecimal lateCharge(Claim claim, BigDecimal figure) {
        if (figure == null || claim.isCorrection() || claim.isWaived() || !overdue(claim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return chargeByBoundary(claim, figure);
    }

    /** The late percent in force on each full 30-day boundary past the claim's due-by plus any grace
     *  ({@link #lateFrom}), up to the sheet's today, of the figure plus the charges of the boundaries before it, each
     *  rounded like its due, added up. */
    private BigDecimal chargeByBoundary(Claim claim, BigDecimal figure) {
        LocalDate from = lateFrom(claim);
        long periods = ChronoUnit.DAYS.between(from, today) / 30;
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        for (long k = 1; k <= periods; k++) {
            BigDecimal pct = sheet.latePctOn(from.plusDays(30 * k));
            if (pct.signum() != 0) {
                charge = charge.add(figure.add(charge).multiply(pct).movePointLeft(2).setScale(2, rounding(claim)));
            }
        }
        return charge;
    }

    /** A rate figure: the amount times the percent, divided by 100, rounded as given. */
    public static BigDecimal rateFigure(BigDecimal amount, BigDecimal pct, RoundingMode rounding) {
        return amount.multiply(pct).movePointLeft(2).setScale(2, rounding);
    }

    /** The floor, applied before the late charge: the greater of the sheet's floor in force on the claim's date and
     *  the given figure; a correction takes no floor. */
    private BigDecimal floored(Claim claim, BigDecimal due) {
        if (claim.isCorrection()) {
            return due;
        }
        BigDecimal floor = sheet.floorOn(claim.getDate());
        return floor != null && due.compareTo(floor) < 0 ? floor.setScale(2) : due;
    }

    /** Whether a claim counts toward the totals on a day: only OPEN, PENDING and SETTLED claims do, but not an OPEN
     *  one deferred until after that day. */
    public static boolean counts(Claim claim, LocalDate today) {
        return (claim.getStatus().isOpen() || claim.getStatus() == ClaimStatus.SETTLED) && !claim.isDeferredOn(today);
    }

    /** Whether a claim counts toward the live totals: as {@link #counts}, except an OPEN or PENDING claim that is
     *  held, and its person's OPEN and PENDING claims dated within a hold of theirs, or all of them once their retirement date is reached, unless a statement
     *  binds them (null person: no holds). */
    public static boolean countsLive(Claim claim, Person person, LocalDate today) {
        return counts(claim, today) && !(claim.getStatus().isOpen() && !claim.isBound() && leftOn(claim, person, today));
    }

    /** Whether a claim counts toward claimed: as {@link #countsLive}, except a hold (the claim's or its person's)
     *  does not take it out; only its person's retirement date being reached does (null person: not retired). */
    public static boolean countsClaimed(Claim claim, Person person, LocalDate today) {
        return counts(claim, today) && !(claim.getStatus().isOpen() && !claim.isBound()
                && person != null && person.retiredBy(today));
    }

    /** Whether a claim counts toward a period's live totals: as {@link #countsLive}, taking the status it had at the
     *  end of the period's last day (null: it had not been added by then, so it does not count); a hold takes out
     *  only a claim that is still OPEN or PENDING and unbound now. */
    public static boolean countsAsOf(Claim claim, ClaimStatus then, Person person, LocalDate today) {
        if (then == null || !(then.isOpen() || then == ClaimStatus.SETTLED)) {
            return false;
        }
        boolean deferred = then == ClaimStatus.OPEN && claim.getDeferredUntil() != null && claim.getDeferredUntil().isAfter(today);
        return !deferred && !(claim.getStatus().isOpen() && !claim.isBound() && leftOn(claim, person, today));
    }

    /** Whether the claim, while OPEN or PENDING and unbound, leaves the live totals on the day: it is held itself, or
     *  its person's claims on its date do ({@link Person#leftOn}; null person: no holds). */
    private static boolean leftOn(Claim claim, Person person, LocalDate today) {
        return claim.isHeld() || (person != null && person.leftOn(claim.getDate(), today));
    }

    /** The percent of the grade's rate in force on a date; none when that rate's to-date is before it. When the
     *  sheet has none in force then, the next sheet up its chain's (the rates of each sheet come in chain order). */
    private Optional<BigDecimal> rateOn(String grade, LocalDate on) {
        return rateOn(grade, on, rates);
    }

    /** The percent of the grade's rate that applies to the claim on its date, as {@link #rateOn(String, LocalDate)}:
     *  a forward-only rate saved after the claim was added does not apply to it ({@link Rate#forClaim}). */
    private Optional<BigDecimal> rateOn(String grade, Claim claim) {
        return rateOn(grade, claim.getDate(), rates.stream().flatMap(r -> r.forClaim(claim.getId()).stream()).toList());
    }

    private static Optional<BigDecimal> rateOn(String grade, LocalDate on, List<Rate> rates) {
        List<String> chain = new ArrayList<>();
        rates.stream().map(Rate::getSheetId).filter(id -> !chain.contains(id)).forEach(chain::add);
        for (String sheetId : chain) {
            Optional<BigDecimal> pct = rates.stream()
                    .filter(r -> r.getSheetId().equals(sheetId) && r.getGrade().equals(grade))
                    .filter(r -> r.getFromDate() == null || !r.getFromDate().isAfter(on))
                    .max(Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .filter(r -> !r.endedBy(on))
                    .map(Rate::getPct);
            if (pct.isPresent()) {
                return pct;
            }
        }
        return Optional.empty();
    }

    /** The due the claim would have today as an open claim bound by no statement: its live figure at today's rates,
     *  grades and floor, plus the late charge it would carry on that as an open claim; null when it has none (n/a). */
    public BigDecimal openDue(Claim claim) {
        BigDecimal figure = liveFigure(claim);
        if (figure == null) {
            return null;
        }
        if (claim.isCorrection() || claim.isWaived() || !lateFrom(claim).isBefore(today)) {
            return floored(claim, figure);
        }
        BigDecimal floored = floored(claim, figure);
        return floored.add(chargeByBoundary(claim, floored));
    }

    /** The claim's due: its frozen due when settled, else the due its person's last statement bound it to, else the
     *  greater of its floor and its figure, plus the late charge worked out on that; null when it has none (n/a). */
    public BigDecimal due(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        BigDecimal figure = figure(claim);
        if (figure == null) {
            return null;
        }
        BigDecimal floored = floored(claim, figure);
        return floored.add(lateCharge(claim, floored));
    }

    /** The claim's figure, without any late charge or floor (a bound claim's: the due it is bound to); null when it
     *  has none (n/a). */
    public BigDecimal figure(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        return liveFigure(claim);
    }

    /** The claim's live figure: neither frozen nor bound; null when it has none (n/a). */
    private BigDecimal liveFigure(Claim claim) {
        return rateOn(grades.on(claim.getDate()), claim)
                .map(pct -> rateFigure(claim.getAmount(), pct, rounding(claim)))
                .orElse(null);
    }

    /** The person's due-total: their counted claims' dues (with any late charge) added up, 0.00 with none; n/a when their grade
     *  in force on the day has no rate in force then, unless a counted claim keeps a figure: a fixed one (settled or bound), or a
     *  live one at another grade they had on its date, or one dated on or before the day (at a rate that has ended since). */
    public String dueTotal(List<Claim> claims, LocalDate today) {
        return dueTotal(claims, today, null);
    }

    /** The person's live due-total, as {@link #dueTotal(List, LocalDate)}; their OPEN claims dated within a hold
     *  of theirs do not count, unless a statement binds them. At most the sheet's cap (n/a is not capped). */
    public String dueTotal(List<Claim> claims, LocalDate today, Person person) {
        String sum = uncappedDueTotal(claims, today, person);
        if (cap == null || "n/a".equals(sum) || new BigDecimal(sum).compareTo(cap) <= 0) {
            return sum;
        }
        return cap.setScale(2).toPlainString();
    }

    /** The person's live due-total before the cap: the sum of their figures. */
    private String uncappedDueTotal(List<Claim> claims, LocalDate today, Person person) {
        List<Claim> counted = claims.stream().filter(c -> countsLive(c, person, today)).toList();
        String grade = grades.on(today);
        boolean kept = counted.stream().anyMatch(c -> c.getDue() != null || (c.isBound() && c.getStatedDue() != null)
                || (!c.isBound() && (!grade.equals(grades.on(c.getDate())) || !c.getDate().isAfter(today)) && figure(c) != null));
        if (rateOn(grade, today).isEmpty() && !kept) {
            return "n/a";
        }
        return counted.stream().map(this::due).filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2).toPlainString();
    }
}

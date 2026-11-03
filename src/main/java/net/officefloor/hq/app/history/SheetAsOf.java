package net.officefloor.hq.app.history;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.Hold;

/** A sheet as it stood at the end of a day: what the changes made while the sheet's today was on or
 *  before that day left (see {@link HistoryRepository#asOf}). The day stands in for the sheet's today in
 *  every figure, worked out by the same rules as the live ones in ClaimRepository: the grade and rate in
 *  force, the rounding, the floor, late charges, holds and retirements.
 *
 *  @param claims        each claim's values then, by claim id (a claim added later is absent)
 *  @param grades        each person's grades then, by person id, each by from-date (null: from the beginning of time)
 *  @param rates         each grade's rates then, by grade, each by from-date
 *  @param settings      each dated setting's values then, by key, each by from-date
 *  @param policyDate    the policy date then (null: blank, the day itself)
 *  @param holds         each person's holds then, by person id
 *  @param retiredFrom   the date each person was retired from then, by person id (absent: not retired)
 *  @param correctedDate the date of a person's claim with a number (person id, number), for a correction's rounding
 */
public record SheetAsOf(LocalDate day, Map<Long, Claim> claims, Map<Long, TreeMap<LocalDate, String>> grades,
        Map<String, TreeMap<LocalDate, Rate>> rates, Map<String, TreeMap<LocalDate, String>> settings,
        LocalDate policyDate, Map<Long, List<Hold>> holds, Map<Long, String> retiredFrom,
        Function<Claim, String> correctedDate) {

    /** A rate then: its percent and to-date (null: with no end). */
    public record Rate(BigDecimal pct, LocalDate to) {
    }

    /** The minimum date, standing for "from the beginning of time" (a null from-date). */
    public static final LocalDate BEGINNING = LocalDate.of(1, 1, 1);

    /** A person's claims then, newest first. */
    public List<Claim> claimsOf(long personId) {
        List<Claim> mine = new ArrayList<>();
        for (Claim c : claims.values()) {
            if (c.personId() == personId) mine.add(c);
        }
        mine.sort((a, b) -> Long.compare(b.id(), a.id()));
        return mine;
    }

    /** A person's holds as they acted on live totals then: once their retirement date was reached they are
     *  held over every date as well. */
    public List<Hold> liveHolds(long personId) {
        List<Hold> all = new ArrayList<>(holds.getOrDefault(personId, List.of()));
        String retired = retiredFrom.get(personId);
        if (retired != null && retired.compareTo(day.toString()) <= 0) all.add(new Hold(null, null));
        return all;
    }

    private static <V> V latest(TreeMap<LocalDate, V> dated, LocalDate on) {
        if (dated == null) return null;
        Map.Entry<LocalDate, V> e = dated.floorEntry(on);
        return e == null ? null : e.getValue();
    }

    /** The grade a person had then on a date. */
    public String gradeOn(long personId, LocalDate on) {
        return latest(grades.get(personId), on);
    }

    /** The rate that would apply then to a person on a date; null when there is none. */
    private Rate rateOn(long personId, LocalDate on) {
        String grade = gradeOn(personId, on);
        return grade == null ? null : latest(rates.get(grade), on);
    }

    /** A dated setting's value then on a date: a list holding the value (null when blank), empty when none was recorded. */
    private List<String> setting(String key, LocalDate on) {
        TreeMap<LocalDate, String> dated = settings.get(key);
        if (dated == null) return List.of();
        Map.Entry<LocalDate, String> e = dated.floorEntry(on);
        if (e == null) return List.of();
        List<String> found = new ArrayList<>();
        found.add(e.getValue());
        return found;
    }

    /** The floor then on a date; 1.00 before any was recorded. */
    public BigDecimal floor(LocalDate on) {
        List<String> found = setting("floor", on);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ONE.setScale(2) : new BigDecimal(found.get(0)).setScale(2);
    }

    /** The late percent then on a date; 0.00 before any was recorded. */
    public BigDecimal latePct(LocalDate on) {
        List<String> found = setting("late-pct", on);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(found.get(0)).setScale(2);
    }

    /** The cap in force on the day; null when there was none. */
    public BigDecimal cap() {
        List<String> found = setting("cap", day);
        return found.isEmpty() || found.get(0) == null ? null : new BigDecimal(found.get(0));
    }

    /** A claim's rate figure then, not floored; null when no rate applied. */
    public BigDecimal due(Claim claim) {
        LocalDate date = LocalDate.parse(claim.date());
        Rate rate = rateOn(claim.personId(), date);
        if (rate == null || (rate.to() != null && rate.to().isBefore(date))) return null;
        return new BigDecimal(claim.amount()).multiply(rate.pct()).movePointLeft(2).setScale(2, rounding(claim));
    }

    /** How a claim's figures rounded then: half up when dated before the policy date (blank: the day), else
     *  half to even; a correction by the date of the claim it corrects. */
    private RoundingMode rounding(Claim claim) {
        String date = claim.date();
        if (claim.corrects() != null) {
            String corrected = correctedDate.apply(claim);
            if (corrected != null) date = corrected;
        }
        LocalDate policy = policyDate != null ? policyDate : day;
        return LocalDate.parse(date).isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** A claim's figure then: its settled due, else the one a statement bound it to (none when bound to
     *  n/a), else its rate figure; null when it had none. */
    public BigDecimal figure(Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        if (claim.bound() && claim.statedNa()) return null;
        if (claim.bound() && claim.statedDue() != null) return new BigDecimal(claim.statedDue());
        return due(claim);
    }

    /** A claim's due then: its settled due, else its figure plus its late charges on the day, at least the
     *  floor (a correction has neither); null when it had no figure. */
    public BigDecimal dueNow(Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        BigDecimal figure = figure(claim);
        if (figure == null || new BigDecimal(claim.amount()).signum() < 0) return figure;
        BigDecimal due = figure.add(lateCharge(claim, figure));
        return due.max(floor(LocalDate.parse(claim.date())));
    }

    /** The day a claim's overdue days counted from then: its due-by plus the grace days, then in force on its date,
     *  of the grade its person had on that date (its due-by when none). */
    public LocalDate overdueFrom(Claim claim) {
        LocalDate dueBy = LocalDate.parse(claim.dueBy());
        LocalDate date = LocalDate.parse(claim.date());
        String grade = gradeOn(claim.personId(), date);
        if (grade == null) return dueBy;
        List<String> found = setting("grace-" + grade, date);
        return found.isEmpty() || found.get(0) == null ? dueBy : dueBy.plusDays(Integer.parseInt(found.get(0)));
    }

    private BigDecimal lateCharge(Claim claim, BigDecimal figure) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (!claim.open() || claim.deferred()) return charge;
        LocalDate dueBy = overdueFrom(claim);
        long periods = dueBy.isBefore(day) ? ChronoUnit.DAYS.between(dueBy, day) / 30 : 0;
        RoundingMode rounding = rounding(claim);
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = latePct(dueBy.plusDays(30 * n));
            if (pct.signum() == 0) continue;
            charge = charge.add(figure.add(charge).multiply(pct).movePointLeft(2).setScale(2, rounding));
        }
        return charge;
    }

    /** Whether the rate that would apply to a claim then had ended before its date. */
    public boolean rateEnded(Claim claim) {
        LocalDate date = LocalDate.parse(claim.date());
        Rate rate = rateOn(claim.personId(), date);
        return rate != null && rate.to() != null && rate.to().isBefore(date);
    }

    /** Whether a rate for the grade a person had on the day was in force on it. */
    public boolean rated(long personId) {
        Rate rate = rateOn(personId, day);
        return rate != null && (rate.to() == null || !rate.to().isBefore(day));
    }
}

package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Settings;

/**
 * The sheet's figures as the screen shows them: each person's claimed, due-total and overdue-total (by person id,
 * in the order they were added), and the sheet's claimed-total, due-total and overdue-total.
 */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal) {

    /** A person's figures: their name, claimed, due-total (n/a when none) and overdue-total. */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal) {
        boolean sameFigures(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }
    }

    /**
     * The sheet's figures now. A person's claimed is the amounts of their claims that count (OPEN
     * and SETTLED, but not an unbound OPEN one dated within one of their holds) added up; their due-total the figures of those claims added up, skipping n/a (a live figure is at least the sheet's floor; an overdue one gains its late
     * charge on {@code today}), and
     * n/a when their grade in force today has no rate today and no claim that counts keeps a fixed
     * figure or has one under the grade in force on its date when that is another grade, or
     * has one dated on or before today (under a rate that has since ended), held to the sheet's cap. Each claim
     * takes the rate of the grade in force on its date. The
     * sheet's totals add these up, skipping n/a; with three or more figures, the due-total and
     * overdue-total leave out the single highest and the single lowest.
     */
    public static Rollup of(String sheetId, LocalDate today, PersonRepository people, ClaimRepository claims,
            RateRepository rates, Settings settings) {
        List<Rate> all = rates.findAll(sheetId);
        List<Claim> sheetClaims = claims.findAll(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        for (Person person : people.findAll(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            boolean fixed = false;
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId()) continue;
                if (!claim.counts(person.heldOn(claim.getDate()), today)) continue;
                claimed = claimed.add(claim.getAmount());
                if (claim.getSettledDue() != null || claim.boundDue() != null) fixed = true;
                String grade = person.gradeOn(claim.getDate());
                BigDecimal figure = ClaimFigure.withLate(claim, all, grade, settings, today);
                if (figure != null) due = due.add(figure);
                if (figure != null && overdue(claim, today)) overdue = overdue.add(figure);
                // A figure found under a grade other than today's is kept, whatever today's rate.
                if (figure != null && !grade.equals(person.gradeOn(today))) fixed = true;
                // So is one dated on or before today, found under a rate that has since ended.
                if (figure != null && !claim.getDate().isAfter(today)) fixed = true;
            }
            boolean none = Rate.inForce(all, person.gradeOn(today), today).isEmpty() && !fixed;
            claimedTotal = claimedTotal.add(claimed);
            overdues.add(overdue);
            due = settings.capped(due, today);
            if (!none) dues.add(due);
            figures.put(person.getId(), new Figures(person.getName(), show(claimed), none ? "n/a" : show(due),
                    show(overdue)));
        }
        return new Rollup(figures, show(claimedTotal), show(trimmed(dues)), show(trimmed(overdues)));
    }

    /** People's figures added up: with three or more, the single highest and single lowest left out. */
    private static BigDecimal trimmed(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(BigDecimal::compareTo);
        if (sorted.size() >= 3) sorted = sorted.subList(1, sorted.size() - 1);
        return sorted.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** Whether a claim is overdue on {@code today}: open (OPEN or PENDING) and past its due-by. */
    private static boolean overdue(Claim claim, LocalDate today) {
        return claim.isOpen() && today != null && claim.getDueBy() != null && claim.getDueBy().isBefore(today);
    }

    /**
     * The audit lines for the move from {@code before} to this: one ROLLUP line per person whose
     * figures changed (a person new since counts as changed), then a TOTALS line when any of the
     * sheet's three totals changed.
     */
    public List<String> changesSince(Rollup before, String sheetId, String user) {
        List<String> lines = new ArrayList<>();
        String head = " sheet=" + sheetId + " by=" + user;
        people.forEach((id, now) -> {
            if (!now.sameFigures(before.people().get(id))) {
                lines.add("ROLLUP" + head + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        });
        if (!claimedTotal.equals(before.claimedTotal()) || !dueTotal.equals(before.dueTotal())
                || !overdueTotal.equals(before.overdueTotal())) {
            lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal
                    + " overdue-total=" + overdueTotal);
        }
        return lines;
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

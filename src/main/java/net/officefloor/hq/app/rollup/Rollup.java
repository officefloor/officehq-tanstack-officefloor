package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.CutTotals;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Settings;

/**
 * The sheet's figures as the screen shows them: each person's claimed, due-total and overdue-total (by person id,
 * in the order they were added), and the sheet's claimed-total, due-total and overdue-total, and the sheet's alert
 * threshold (null: off).
 */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal,
        BigDecimal alertThreshold) {

    /**
     * A person's figures: their name, claimed, due-total (n/a when none) and overdue-total, and their score:
     * the due of their SETTLED claims that count and correct nothing (skipping n/a) divided by 100, minus 5 for every overdue
     * claim of theirs that counts, rounded half to even to a whole number.
     */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal, String score) {
        boolean sameFigures(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }

        /** Whether their due-total is above a threshold (never when they have none). */
        boolean above(BigDecimal threshold) {
            return !"n/a".equals(dueTotal) && new BigDecimal(dueTotal).compareTo(threshold) > 0;
        }
    }

    /**
     * The sheet's figures now. A person's claimed is the amounts of their claims that count (OPEN
     * and SETTLED, held or not, but not an unbound OPEN one once their retirement date is reached, nor a correction) added up; their due-total the figures of the claims that count (held or not, but not an unbound OPEN one once their retirement date is reached) added up, skipping n/a (a live due is the greater of the floor on its date and its rate figure plus the late
     * charge it gains on {@code today}), and
     * n/a when their grade in force today has no rate today and no claim that counts keeps a fixed
     * figure or has one under the grade in force on its date when that is another grade, or
     * has one dated on or before today (under a rate that has since ended), held to the sheet's cap less their cut
     * total (see {@link Settings#capped}). Each claim
     * takes the rate of the grade in force on its date. A shared claim's figure counts less its holders' parts
     * for its own person, and each holder's part (its figure times their share / 100, rounded half to even)
     * for the holder, which keeps the holder's due-total from n/a. The
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
        List<Person> everyone = people.findAll(sheetId);
        Map<Long, List<ClaimShare>> shares = claims.shares(sheetId);
        Map<Long, BigDecimal> held = heldParts(everyone, sheetClaims, shares, all, settings, today);
        Map<Long, BigDecimal> cuts = CutTotals.of(sheetId, today);
        for (Person person : everyone) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            boolean fixed = false;
            BigDecimal settled = BigDecimal.ZERO.setScale(2);
            int overdueCount = 0;
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId()) continue;
                if (!claim.isCorrection() && claim.countsClaimed(person.retiredOn(today), today)) {
                    claimed = claimed.add(claim.getAmount());
                }
                if (!claim.counts(person.heldOn(claim.getDate(), today), today)) continue;
                if (claim.getSettledDue() != null || claim.boundDue() != null) fixed = true;
                String grade = person.gradeOn(claim.getDate());
                BigDecimal figure = ClaimFigure.withLate(claim, all, grade, settings, today);
                if (figure != null) due = due.add(ownPart(figure, shares.get(claim.getId())));
                if (figure != null && overdue(claim, grade, settings, today)) overdue = overdue.add(figure);
                if (figure != null && claim.getStatus() == net.officefloor.hq.app.claim.ClaimStatus.SETTLED
                        && !claim.isCorrection()) {
                    settled = settled.add(figure);
                }
                if (overdue(claim, grade, settings, today)) overdueCount++;
                // A figure found under a grade other than today's is kept, whatever today's rate.
                if (figure != null && !grade.equals(person.gradeOn(today))) fixed = true;
                // So is one dated on or before today, found under a rate that has since ended.
                if (figure != null && !claim.getDate().isAfter(today)) fixed = true;
            }
            // Their parts of others' claims shared with them count, and keep a figure whatever today's rate.
            if (held.containsKey(person.getId())) {
                due = due.add(held.get(person.getId()));
                fixed = true;
            }
            boolean none = Rate.inForce(all, person.gradeOn(today), today).isEmpty() && !fixed;
            claimedTotal = claimedTotal.add(claimed);
            overdues.add(overdue);
            due = settings.capped(due, cuts.getOrDefault(person.getId(), BigDecimal.ZERO), today);
            if (!none) dues.add(due);
            figures.put(person.getId(), new Figures(person.getName(), show(claimed), none ? "n/a" : show(due),
                    show(overdue), score(settled, overdueCount)));
        }
        return new Rollup(figures, show(claimedTotal), show(trimmed(dues)), show(trimmed(overdues)),
                settings.alertThreshold());
    }

    /**
     * Each holder's parts (by person id) of the claims shared with them that count for their own person:
     * the claim's figure times their share divided by 100, rounded half to even, added up. A holder of
     * no part of a figure is absent.
     */
    private static Map<Long, BigDecimal> heldParts(List<Person> everyone, List<Claim> sheetClaims,
            Map<Long, List<ClaimShare>> shares, List<Rate> all, Settings settings, LocalDate today) {
        Map<Long, BigDecimal> held = new HashMap<>();
        for (Person owner : everyone) {
            for (Claim claim : sheetClaims) {
                List<ClaimShare> of = shares.get(claim.getId());
                if (of == null || claim.getPersonId() != owner.getId()) continue;
                if (!claim.counts(owner.heldOn(claim.getDate(), today), today)) continue;
                BigDecimal figure = ClaimFigure.withLate(claim, all, owner.gradeOn(claim.getDate()), settings, today);
                if (figure == null) continue;
                for (ClaimShare share : of) held.merge(share.personId(), share.partOf(figure), BigDecimal::add);
            }
        }
        return held;
    }

    /** A claim's figure less its holders' parts (the whole figure when it has no shares). */
    private static BigDecimal ownPart(BigDecimal figure, List<ClaimShare> shares) {
        if (shares == null) return figure;
        BigDecimal own = figure;
        for (ClaimShare share : shares) own = own.subtract(share.partOf(figure));
        return own;
    }

    /** A score: settled due / 100, minus 5 per overdue claim, rounded half to even to a whole number. */
    private static String score(BigDecimal settled, int overdueCount) {
        return settled.movePointLeft(2).subtract(BigDecimal.valueOf(5L * overdueCount))
                .setScale(0, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }

    /** People's figures added up: with three or more, the single highest and single lowest left out. */
    private static BigDecimal trimmed(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(BigDecimal::compareTo);
        if (sorted.size() >= 3) sorted = sorted.subList(1, sorted.size() - 1);
        return sorted.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /**
     * Whether a claim is overdue on {@code today}: open (OPEN or PENDING) and past its due-by plus the
     * grace days of its grade on its date ({@link Settings#overdueFrom}).
     */
    private static boolean overdue(Claim claim, String grade, Settings settings, LocalDate today) {
        return claim.isOpen() && today != null && claim.getDueBy() != null
                && settings.overdueFrom(claim.getDueBy(), grade, claim.getDate()).isBefore(today);
    }

    /**
     * The audit lines for the move from {@code before} to this: one ROLLUP line per person whose
     * figures changed (a person new since counts as changed), then a TOTALS line when any of the
     * sheet's three totals changed, then, while the alert threshold is set, one ALERT line per person, in
     * list order, whose due-total went from at or below it (or none, or not there before) to above it.
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
        if (alertThreshold != null) {
            people.forEach((id, now) -> {
                Figures was = before.people().get(id);
                if (now.above(alertThreshold) && (was == null || !was.above(alertThreshold))) {
                    lines.add("ALERT" + head + " name=" + now.name() + " due-total=" + now.dueTotal());
                }
            });
        }
        return lines;
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

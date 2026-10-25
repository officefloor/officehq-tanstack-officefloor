package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The sheet's figures at one moment, as the screen shows them: each person's claimed (their counted claims, not corrections) and due-total, in the
 * order they were added, and the sheet's claimed-total and due-total (everyone's added up, skipping n/a), and each
 * person's and the sheet's overdue-total (their counted overdue claims' dues, with late charge, skipping n/a).
 * The sheet's due-total and overdue-total leave out the single highest and lowest figure when there are three or more.
 * A person's OPEN and PENDING claims dated within a hold of theirs, and held claims, still count in claimed but in
 * no due or overdue figure; all of them, once their retirement date is reached, count in none of them; unless a
 * statement binds them. The sheet's alert threshold (null: off) is kept with them, as it stands now.
 */
public class Rollup {

    /** One person's figures. */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal) {
    }

    private final Map<Long, Figures> people;
    private final String claimedTotal;
    private final String dueTotal;
    private final String overdueTotal;
    private final BigDecimal alertThreshold;

    private Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal,
            BigDecimal alertThreshold) {
        this.people = people;
        this.alertThreshold = alertThreshold;
        this.claimedTotal = claimedTotal;
        this.dueTotal = dueTotal;
        this.overdueTotal = overdueTotal;
    }

    /** Works out the sheet's figures now; a sheet not yet stored has none. */
    public static Rollup of(String sheetId, SheetRepository sheets, PersonRepository persons,
            ClaimRepository claims, RateRepository rates, PersonGradeRepository grades) {
        String id = sheetId.trim();
        Optional<Sheet> sheet = sheets.findById(id);
        Map<Long, Figures> people = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO;
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        if (sheet.isPresent()) {
            List<Claim> all = claims.findBySheetIdOrderByIdDesc(id);
            var sheetRates = rates.findInChain(id);
            Map<Long, List<PersonGrade>> dated = grades.datedOnSheet(id);
            for (Person person : persons.findListed(id)) {
                List<Claim> mine = all.stream().filter(c -> person.getId().equals(c.getPersonId())).toList();
                BigDecimal claimed = mine.stream()
                        .filter(c -> c.getCorrects() == null && ClaimFigures.countsClaimed(c, person, sheet.get().getToday())).map(Claim::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
                ClaimFigures figures = new ClaimFigures(sheetRates,
                        new PersonGrades(person.getGrade(), dated.getOrDefault(person.getId(), List.of())), sheet.get());
                String due = figures.dueTotal(mine, sheet.get().getToday(), person);
                BigDecimal overdue = mine.stream()
                        .filter(c -> ClaimFigures.countsLive(c, person, sheet.get().getToday()) && figures.overdue(c))
                        .map(figures::due).filter(d -> d != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
                people.put(person.getId(), new Figures(person.getName(), claimed.toPlainString(), due,
                        overdue.toPlainString()));
                overdues.add(overdue);
                claimedTotal = claimedTotal.add(claimed);
                if (!"n/a".equals(due)) {
                    dues.add(new BigDecimal(due));
                }
            }
        }
        return new Rollup(people, claimedTotal.setScale(2).toPlainString(), trimmedSum(dues),
                trimmedSum(overdues), sheet.map(Sheet::getAlertThreshold).orElse(null));
    }

    /** Adds up people's figures; with three or more, the single highest and the single lowest are left out. */
    private static String trimmedSum(List<BigDecimal> figures) {
        List<BigDecimal> sorted = figures.stream().sorted().toList();
        List<BigDecimal> kept = sorted.size() >= 3 ? sorted.subList(1, sorted.size() - 1) : sorted;
        return kept.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2).toPlainString();
    }

    /** One SNAPSHOT line per person, in list order, with the due-total as it stands in these figures. */
    public List<String> snapshot(String sheetId, String user) {
        String head = " sheet=" + sheetId.trim() + " by=" + user;
        List<String> lines = new ArrayList<>();
        people.values().forEach(f -> lines.add("SNAPSHOT" + head + " name=" + f.name() + " due-total=" + f.dueTotal()));
        return lines;
    }

    /** Each person's due-total as it stands in these figures, by their id, in list order. */
    public Map<Long, String> dueTotals() {
        Map<Long, String> dues = new LinkedHashMap<>();
        people.forEach((id, f) -> dues.put(id, f.dueTotal()));
        return dues;
    }

    /** The audit lines stating the figures in full: one per person, in list order, then the totals. */
    public List<String> lines(String sheetId, String user) {
        String head = " sheet=" + sheetId.trim() + " by=" + user;
        List<String> lines = new ArrayList<>();
        people.values().forEach(f -> lines.add("ROLLUP" + head + " person=" + f.name() + " claimed=" + f.claimed()
                + " due-total=" + f.dueTotal() + " overdue-total=" + f.overdueTotal()));
        lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal
                + " overdue-total=" + overdueTotal);
        lines.addAll(alertsSince(Map.of(), head));
        return lines;
    }

    /** One ALERT line per person, in list order, whose due-total is now above the alert threshold and before was at
     *  or below it (or they had no due-total); none when the threshold is off. */
    private List<String> alertsSince(Map<Long, Figures> before, String head) {
        List<String> lines = new ArrayList<>();
        if (alertThreshold == null) {
            return lines;
        }
        people.forEach((id, now) -> {
            Figures was = before.get(id);
            if (above(now.dueTotal()) && (was == null || !above(was.dueTotal()))) {
                lines.add("ALERT" + head + " name=" + now.name() + " due-total=" + now.dueTotal());
            }
        });
        return lines;
    }

    private boolean above(String dueTotal) {
        return !"n/a".equals(dueTotal) && new BigDecimal(dueTotal).compareTo(alertThreshold) > 0;
    }

    /** The audit lines telling how the figures moved from before to now: one per person whose figures changed
     *  (or who is new), then the totals when they changed, then the ALERT lines of those who crossed above the alert
     *  threshold; none when nothing moved. */
    public List<String> changesSince(Rollup before, String sheetId, String user) {
        String head = " sheet=" + sheetId.trim() + " by=" + user;
        List<String> lines = new ArrayList<>();
        people.forEach((id, now) -> {
            Figures was = before.people.get(id);
            if (was == null || !was.claimed().equals(now.claimed()) || !was.dueTotal().equals(now.dueTotal())
                    || !was.overdueTotal().equals(now.overdueTotal())) {
                lines.add("ROLLUP" + head + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        });
        if (!before.claimedTotal.equals(claimedTotal) || !before.dueTotal.equals(dueTotal)
                || !before.overdueTotal.equals(overdueTotal)) {
            lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal
                    + " overdue-total=" + overdueTotal);
        }
        lines.addAll(alertsSince(before.people, head));
        return lines;
    }
}

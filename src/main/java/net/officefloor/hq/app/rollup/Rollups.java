package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;
import org.springframework.stereotype.Service;

/** Takes the sheet's figures, and records in the audit file how they moved over a change: a ROLLUP line per
 *  person whose figures (claimed, due-total, overdue-total) changed (a person new to the sheet counting as changed), in the order they were added,
 *  then a TOTALS line when the sheet's totals changed (its due-total and overdue-total leave out the single highest and
 *  single lowest person's figure when three or more people have one; n/a is no figure), then, when the sheet has an
 *  alert threshold, an ALERT line per person whose due-total went from at or below it (or none) to above it, in
 *  list order. A change that moves no figure records nothing. */
@Service
public class Rollups {

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;
    private final Audit audit;

    public Rollups(PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures as they are now. */
    public Rollup take(String sheetId) {
        var today = sheets.today(sheetId);
        var floor = settings.floor(sheetId);
        BigDecimal cap = settings.cap(sheetId);
        LateCharge late = LateCharge.of(sheetId, settings, sheets, claims, people);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = new BigDecimal("0.00");
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        for (Person p : people.list(sheetId)) {
            List<Claim> theirs = all.stream().filter(c -> c.personId() == p.id()).toList();
            // claimed: the amounts of their claims that count toward claimed (a held one does), leaving out corrections of another claim
            BigDecimal claimed = theirs.stream()
                    .filter(c -> Dues.countsClaimed(c, p.retiredFrom(), today) && c.corrects() == null)
                    .map(Claim::amount).reduce(new BigDecimal("0.00"), BigDecimal::add);
            BigDecimal due = Dues.capped(
                    Dues.total(sheetId, theirs, p.grades(), p.holds(), p.retiredFrom(), today, rates, floor, late), cap);
            // overdue-total: the dues (as shown, late charge included) of their overdue claims that count, skipping n/a
            BigDecimal overdue = theirs.stream()
                    .filter(c -> Dues.counts(c, p.holds(), p.retiredFrom(), today) && late.overdue(c))
                    .map(c -> Dues.shown(c, p.grades(), rates, floor, late)).filter(Objects::nonNull)
                    .reduce(new BigDecimal("0.00"), BigDecimal::add);
            claimedTotal = claimedTotal.add(claimed);
            overdues.add(overdue);
            if (due != null) {
                dues.add(due);
            }
            figures.put(p.id(), new Rollup.Figures(p.name(), shown(claimed), due != null ? shown(due) : "n/a",
                    shown(overdue)));
        }
        return new Rollup(figures, shown(claimedTotal), shown(trimmed(dues)), shown(trimmed(overdues)));
    }

    /** A person's name and score. */
    public record Score(String name, String score) {
    }

    /** Each person's score as it is now, in list order: their settled figures (the due, as shown, of each SETTLED
     *  claim that counts, skipping n/a) divided by 100, minus 5 for each of their overdue claims that count, rounded
     *  half to even to a whole number. */
    public List<Score> scores(String sheetId) {
        var today = sheets.today(sheetId);
        var floor = settings.floor(sheetId);
        LateCharge late = LateCharge.of(sheetId, settings, sheets, claims, people);
        List<Claim> all = claims.list(sheetId);
        List<Score> scores = new ArrayList<>();
        for (Person p : people.list(sheetId)) {
            List<Claim> counted = all.stream()
                    .filter(c -> c.personId() == p.id() && Dues.counts(c, p.holds(), p.retiredFrom(), today)).toList();
            BigDecimal settled = counted.stream().filter(c -> c.status() == ClaimStatus.SETTLED)
                    .map(c -> Dues.shown(c, p.grades(), rates, floor, late)).filter(Objects::nonNull)
                    .reduce(new BigDecimal("0.00"), BigDecimal::add);
            long overdue = counted.stream().filter(late::overdue).count();
            BigDecimal score = settled.movePointLeft(2).subtract(BigDecimal.valueOf(5 * overdue))
                    .setScale(0, RoundingMode.HALF_EVEN);
            scores.add(new Score(p.name(), score.toPlainString()));
        }
        return scores;
    }

    /** Record how the sheet's figures moved since `before`, as a change made by `user`. */
    public void record(String sheetId, String user, Rollup before) {
        Rollup after = take(sheetId);
        for (var e : after.people().entrySet()) {
            Rollup.Figures was = before.people().get(e.getKey());
            Rollup.Figures now = e.getValue();
            if (was == null || !Objects.equals(was.claimed(), now.claimed())
                    || !Objects.equals(was.dueTotal(), now.dueTotal())
                    || !Objects.equals(was.overdueTotal(), now.overdueTotal())) {
                audit.record("ROLLUP sheet=" + sheetId + " by=" + user + " person=" + now.name()
                        + " claimed=" + now.claimed() + " due-total=" + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        }
        if (!before.claimedTotal().equals(after.claimedTotal()) || !before.dueTotal().equals(after.dueTotal())
                || !before.overdueTotal().equals(after.overdueTotal())) {
            audit.record("TOTALS sheet=" + sheetId + " by=" + user + " claimed-total=" + after.claimedTotal()
                    + " due-total=" + after.dueTotal() + " overdue-total=" + after.overdueTotal());
        }
        BigDecimal threshold = settings.alertThreshold(sheetId);
        if (threshold != null) {
            for (var e : after.people().entrySet()) {
                Rollup.Figures was = before.people().get(e.getKey());
                Rollup.Figures now = e.getValue();
                if (!above(was != null ? was.dueTotal() : null, threshold) && above(now.dueTotal(), threshold)) {
                    audit.record("ALERT sheet=" + sheetId + " by=" + user + " name=" + now.name()
                            + " due-total=" + now.dueTotal());
                }
            }
        }
    }

    /** Whether a due-total as shown (n/a or none: no due-total) is above the threshold. */
    private static boolean above(String dueTotal, BigDecimal threshold) {
        return dueTotal != null && !"n/a".equals(dueTotal) && new BigDecimal(dueTotal).compareTo(threshold) > 0;
    }

    /** Per-person figures added up, leaving out the single highest and the single lowest when there are three or more. */
    private static BigDecimal trimmed(List<BigDecimal> figures) {
        List<BigDecimal> sorted = figures.stream().sorted().toList();
        if (sorted.size() >= 3) {
            sorted = sorted.subList(1, sorted.size() - 1);
        }
        return sorted.stream().reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    private static String shown(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }
}

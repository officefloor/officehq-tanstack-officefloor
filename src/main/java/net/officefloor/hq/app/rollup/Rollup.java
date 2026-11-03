package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/** The sheet's figures as shown: each person's claimed, due-total and overdue-total, and the sheet's
 *  claimed-total, due-total and overdue-total. A change takes them before it writes, then after its own audit lines records one ROLLUP
 *  line per person whose figures moved (people in list order; a new person counts as
 *  moved), then a TOTALS line when the sheet's totals moved, then, while the sheet has an alert threshold, one ALERT
 *  line per person (in list order) whose due-total went from at or below it (or none) to above it. */
@Service
public class Rollup {

    /** A person's figures: claimed (two decimals), due-total (two decimals, or 'n/a') and overdue-total
     *  (two decimals: the sum of the dues now of their counting claims that are overdue, skipping those with none), and
     *  score (the sum of the dues now of their counting SETTLED claims that correct nothing divided by 100, rounded half to even to a whole
     *  number, minus 5 for every counting claim of theirs that is overdue). */
    record Figures(String name, String claimed, String dueTotal, String overdueTotal, long score) {
        boolean same(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal)
                    && overdueTotal.equals(other.overdueTotal);
        }
    }

    /** The figures of a sheet at one moment, people by id in list order. */
    public record Snapshot(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal) {
    }

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final SheetRepository sheets;
    private final SettingsRepository settings;
    private final Audit audit;

    public Rollup(PersonRepository people, ClaimRepository claims, SheetRepository sheets, SettingsRepository settings,
            Audit audit) {
        this.settings = settings;
        this.people = people;
        this.claims = claims;
        this.sheets = sheets;
        this.audit = audit;
    }

    /** The sheet's figures now. A person's claimed is the sum of their counting claims' amounts, leaving out corrections
     *  (claims that correct another). A person's due-total is the sum of their counting claims' dues now (late charges included),
     *  skipping those with none; 'n/a' when no rate for the grade they have on the sheet's today is in force
     *  then and no counting claim keeps a fixed figure or has a due (at the grade it was claimed under). A due-total is at most the sheet's cap (an 'n/a' is not capped). The sheet's due-total skips 'n/a'.
     *  The sheet's due-total and overdue-total leave out the single highest and lowest person's figure when three or more
     *  people have one. */
    public Snapshot take(String sheetId) {
        List<Person> everyone = people.list(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        if (!everyone.isEmpty()) {
            LocalDate today = sheets.today(sheetId);
            List<Claim> all = claims.list(sheetId);
            BigDecimal cap = settings.cap(sheetId);
            for (Person p : everyone) {
                BigDecimal claimed = BigDecimal.ZERO.setScale(2);
                BigDecimal due = BigDecimal.ZERO.setScale(2);
                BigDecimal overdue = BigDecimal.ZERO.setScale(2);
                BigDecimal settled = BigDecimal.ZERO.setScale(2);
                int overdueCount = 0;
                boolean fixed = false;
                for (Claim c : all) {
                    if (c.personId() != p.id()) continue;
                    String on = today == null ? null : today.toString();
                    if (c.corrects() == null && c.countsClaimed(p.claimedHolds(on))) claimed = claimed.add(new BigDecimal(c.amount()));
                    if (!c.counts(p.liveHolds(on))) continue;
                    fixed |= c.fixed();
                    BigDecimal now = claims.dueNow(sheetId, c);
                    if (now != null) due = due.add(now);
                    if (now != null && claims.overdue(sheetId, c, today)) overdue = overdue.add(now);
                    if (now != null && "SETTLED".equals(c.status()) && c.corrects() == null) settled = settled.add(now);
                    if (claims.overdue(sheetId, c, today)) overdueCount++;
                    fixed |= now != null;
                }
                boolean shown = fixed || claims.rated(sheetId, p.id(), today);
                due = SettingsRepository.capped(due, cap);
                figures.put(p.id(), new Figures(p.name(), claimed.toPlainString(),
                        shown ? due.toPlainString() : "n/a", overdue.toPlainString(),
                        settled.movePointLeft(2).setScale(0, RoundingMode.HALF_EVEN).longValueExact() - 5L * overdueCount));
                claimedTotal = claimedTotal.add(claimed);
                if (shown) dues.add(due);
                overdues.add(overdue);
            }
        }
        return new Snapshot(figures, claimedTotal.toPlainString(), trimmed(dues).toPlainString(),
                trimmed(overdues).toPlainString());
    }

    /** People's figures added up, leaving out the single highest and the single lowest when there are three or more. */
    static BigDecimal trimmed(List<BigDecimal> figures) {
        List<BigDecimal> sorted = new ArrayList<>(figures);
        Collections.sort(sorted);
        if (sorted.size() >= 3) sorted = sorted.subList(1, sorted.size() - 1);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (BigDecimal f : sorted) total = total.add(f);
        return total;
    }

    /** Record one SNAPSHOT line per person (in list order) with their due-total as {@code before} had it,
     *  ending with for=<steward> when {@code by} made the change under that steward's delegation (null when not),
     *  and keep it on the person with the day being left. */
    public void snapshot(String sheetId, String by, Snapshot before, String delegator, LocalDate dayLeft) {
        for (Map.Entry<Long, Figures> e : before.people().entrySet()) {
            Figures f = e.getValue();
            people.snapshot(sheetId, e.getKey(), dayLeft, f.dueTotal());
            audit.record("SNAPSHOT sheet=" + sheetId + " by=" + by + " name=" + f.name() + " due-total=" + f.dueTotal()
                    + (delegator == null ? "" : " for=" + delegator));
        }
    }

    /** Record one SCORE line per person (in list order) with their score as {@code before} had it. */
    public void scores(String sheetId, String by, Snapshot before) {
        for (Figures f : before.people().values()) {
            audit.record("SCORE sheet=" + sheetId + " by=" + by + " name=" + f.name() + " score=" + f.score());
        }
    }

    /** Record the lines for every person (in list order) and the sheet's totals, moved or not. */
    public void recordAll(String sheetId, String by) {
        Map<Long, Figures> none = Map.of();
        record(sheetId, by, new Snapshot(none, "", "", ""));
    }

    /** Record the lines for whatever figures moved since {@code before}. */
    public void record(String sheetId, String by, Snapshot before) {
        Snapshot after = take(sheetId);
        for (Map.Entry<Long, Figures> e : after.people().entrySet()) {
            Figures now = e.getValue();
            if (now.same(before.people().get(e.getKey()))) continue;
            audit.record("ROLLUP sheet=" + sheetId + " by=" + by + " person=" + now.name() + " claimed="
                    + now.claimed() + " due-total=" + now.dueTotal() + " overdue-total=" + now.overdueTotal());
        }
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())
                || !after.overdueTotal().equals(before.overdueTotal())) {
            audit.record("TOTALS sheet=" + sheetId + " by=" + by + " claimed-total=" + after.claimedTotal()
                    + " due-total=" + after.dueTotal() + " overdue-total=" + after.overdueTotal());
        }
        BigDecimal threshold = settings.alertThreshold(sheetId);
        if (threshold == null) return;
        for (Map.Entry<Long, Figures> e : after.people().entrySet()) {
            Figures now = e.getValue();
            Figures was = before.people().get(e.getKey());
            if (above(now, threshold) && !above(was, threshold)) {
                audit.record("ALERT sheet=" + sheetId + " by=" + by + " name=" + now.name() + " due-total=" + now.dueTotal());
            }
        }
    }

    /** Whether a person's due-total is above the threshold (no figures, or an 'n/a', is not). */
    private static boolean above(Figures f, BigDecimal threshold) {
        return f != null && !"n/a".equals(f.dueTotal()) && new BigDecimal(f.dueTotal()).compareTo(threshold) > 0;
    }
}

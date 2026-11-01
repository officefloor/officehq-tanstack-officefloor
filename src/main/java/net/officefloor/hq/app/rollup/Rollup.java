package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/**
 * The figures the screen shows — each person's claimed and due-total, the sheet's claimed-total and
 * due-total, and each one's overdue-total and the sheet's — worked out as the screen does, so a change can audit the ones it moved: one ROLLUP
 * line per person whose figures changed (a new person counts as changed), then a TOTALS line when
 * the sheet's totals changed, then, with an alert threshold set, an ALERT line per person (in list
 * order) whose due-total went from at or below it (or none) to above it.
 */
@Service
public class Rollup {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final People people;
    private final Claims claims;
    private final Figures figures;
    private final Rates rates;
    private final Sheets sheets;
    private final Audit audit;
    private final Settings settings;

    public Rollup(People people, Claims claims, Figures figures, Rates rates, Sheets sheets, Audit audit, Settings settings) {
        this.people = people;
        this.claims = claims;
        this.figures = figures;
        this.rates = rates;
        this.sheets = sheets;
        this.audit = audit;
        this.settings = settings;
    }

    /** The sheet's figures now. A person's due-total is the sum of their counting claims' figures, less
     *  the parts their share holders carry, plus their parts of the counting claims shared with them (a
     *  person's OPEN claims once today reaches their retirement date do not count, unless a statement
     *  binds them; a hold takes nothing out),
     *  n/a when there is no rate today and none has a figure, at most the sheet's cap (n/a is not
     *  capped); the sheet's skips the n/a ones, and its due and overdue totals leave out the single
     *  highest and lowest person's figure when three or more have one. */
    public Snapshot snapshot(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.onSheet(sheetId);
        Map<Long, Snapshot.PersonFigures> figuresOf = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        List<Person> everyone = people.onSheet(sheetId);
        // Each counting claim's figure (by its own person's grade), for the parts its share holders carry.
        Map<Long, BigDecimal> shared = new java.util.HashMap<>();
        for (Claim claim : all) {
            if (claim.getShares().isEmpty()) continue;
            Person owner = everyone.stream().filter(p -> p.getId() == claim.getPersonId()).findFirst().orElse(null);
            if (owner == null || !owner.counts(claim)) continue;
            BigDecimal figure = figures.due(claim, owner.gradeOn(claim.getDate()));
            if (figure != null) shared.put(claim.getId(), figure);
        }
        for (Person person : everyone) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            BigDecimal overdue = ZERO;
            BigDecimal settled = ZERO;
            int overdueCount = 0;
            boolean any = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId()) continue;
                if (person.countsClaimed(claim) && claim.getCorrects() == null) claimed = claimed.add(claim.getAmount());
                if (!person.counts(claim)) continue;
                String grade = person.gradeOn(claim.getDate());
                BigDecimal figure = figures.due(claim, grade);
                if (figures.overdue(claim, grade)) overdueCount++;
                if (figure != null && claim.getStatus() == ClaimStatus.SETTLED
                        && claim.getCorrects() == null) settled = settled.add(figure);
                if (figure != null) {
                    // Less the parts its share holders carry.
                    BigDecimal kept = figure;
                    for (ClaimShare share : claim.getShares()) kept = kept.subtract(share.partOf(figure));
                    due = due.add(kept);
                    any = true;
                    if (figures.overdue(claim, grade)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            // Their parts of the claims shared with them.
            for (Claim claim : all) {
                BigDecimal figure = shared.get(claim.getId());
                if (figure == null) continue;
                for (ClaimShare share : claim.getShares()) {
                    if (share.personId() != person.getId()) continue;
                    due = due.add(share.partOf(figure));
                    any = true;
                }
            }
            if (!any && rates.pct(sheetId, person.gradeOn(today), today) == null) due = null;
            due = settings.capped(sheetId, due);
            figuresOf.put(person.getId(), new Snapshot.PersonFigures(person.getName(), claimed, due, overdue,
                    settled.subtract(BigDecimal.valueOf(500L * overdueCount)).divide(HUNDRED, 0, RoundingMode.HALF_EVEN)));
            claimedTotal = claimedTotal.add(claimed);
            if (due != null) dues.add(due);
            overdues.add(overdue);
        }
        return new Snapshot(figuresOf, claimedTotal, trimmed(dues), trimmed(overdues));
    }

    /** The figures added up, leaving out the single highest and lowest when there are three or more. */
    private static BigDecimal trimmed(List<BigDecimal> figures) {
        List<BigDecimal> sorted = new ArrayList<>(figures);
        sorted.sort(BigDecimal::compareTo);
        if (sorted.size() >= 3) sorted = sorted.subList(1, sorted.size() - 1);
        return sorted.stream().reduce(ZERO, BigDecimal::add);
    }

    /** Audit the figures that moved between `before` and the sheet now. */
    public void record(String sheetId, Snapshot before, String user) {
        record(sheetId, before, user, audit);
    }

    /** Audit the figures that moved between `before` and the sheet now, to the given audit. */
    public void record(String sheetId, Snapshot before, String user, Audit audit) {
        Snapshot after = snapshot(sheetId);
        String prefix = " sheet=" + sheetId + " by=" + user;
        after.people().forEach((id, now) -> {
            Snapshot.PersonFigures was = before.people().get(id);
            if (was != null && same(was.claimed(), now.claimed()) && same(was.dueTotal(), now.dueTotal())
                    && same(was.overdueTotal(), now.overdueTotal())) return;
            audit.record("ROLLUP" + prefix + " person=" + now.name() + " claimed=" + show(now.claimed())
                    + " due-total=" + show(now.dueTotal()) + " overdue-total=" + show(now.overdueTotal()));
        });
        if (!same(before.claimedTotal(), after.claimedTotal()) || !same(before.dueTotal(), after.dueTotal())
                || !same(before.overdueTotal(), after.overdueTotal())) {
            audit.record("TOTALS" + prefix + " claimed-total=" + show(after.claimedTotal())
                    + " due-total=" + show(after.dueTotal()) + " overdue-total=" + show(after.overdueTotal()));
        }
        BigDecimal threshold = settings.alertThreshold(sheetId);
        if (threshold == null) return;
        after.people().forEach((id, now) -> {
            Snapshot.PersonFigures was = before.people().get(id);
            if (above(now.dueTotal(), threshold) && (was == null || !above(was.dueTotal(), threshold))) {
                audit.record("ALERT" + prefix + " name=" + now.name() + " due-total=" + show(now.dueTotal()));
            }
        });
    }

    /** Whether a due-total is above the threshold; n/a never is. */
    private static boolean above(BigDecimal dueTotal, BigDecimal threshold) {
        return dueTotal != null && dueTotal.compareTo(threshold) > 0;
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? Objects.equals(a, b) : a.compareTo(b) == 0;
    }

    private static String show(BigDecimal value) {
        return value == null ? "n/a" : value.setScale(2).toPlainString();
    }
}

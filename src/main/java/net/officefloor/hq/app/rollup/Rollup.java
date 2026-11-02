package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them: each person's claimed, due-total and overdue-total, and the sheet's
 * claimed-total, due-total and overdue-total (the totals leave out the single highest and lowest person's figure when
 * three or more people have one). A change takes them before it writes, then {@link #record} audits the ones it moved: a ROLLUP line
 * per person whose figures changed (in list order, by position; a new person counts as changed), then a TOTALS
 * line when the sheet's totals changed (a shared claim's due counts for each holder their share of it, and for its own
 * person the rest), then an ALERT line per person whose due-total crossed above the sheet's alert
 * threshold (from at or below it, or no due-total), in list order.
 */
@Service
public class Rollup {

    /**
     * A person's figures: claimed (two decimals), due-total (two decimals, or n/a) and overdue-total (the dues of
     * their counted overdue claims, two decimals); and the score (see {@link #score}).
     */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal, long score) {
    }

    /** The sheet's figures at one moment: per person id, in list order, and the totals. */
    public record Snapshot(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal) {
    }

    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final SheetClock clock;
    private final Settings settings;
    private final Audit audit;

    public Rollup(People people, Claims claims, Rates rates, SheetClock clock, Settings settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.clock = clock;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures now. */
    public Snapshot take(String sheet) {
        LocalDate today = clock.today(sheet);
        List<Rate> sheetRates = rates.list(sheet);
        Dated floor = settings.floor(sheet);
        BigDecimal cap = settings.cap(sheet);
        Late late = settings.late(sheet, today);
        List<Claim> all = claims.list(sheet);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> dueFigures = new ArrayList<>();
        List<BigDecimal> overdueFigures = new ArrayList<>();
        List<Person> listed = people.list(sheet);
        Map<Long, Person> byId = new LinkedHashMap<>();
        listed.forEach((p) -> byId.put(p.id(), p));
        for (Person person : listed) {
            List<Claim> mine = all.stream().filter((c) -> c.person() == person.id() && person.counts(c)).toList();
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            BigDecimal settled = BigDecimal.ZERO.setScale(2);
            int overdueCount = 0;
            for (Claim claim : all) {
                if (claim.person() == person.id() && person.countsClaimed(claim) && !claim.correction()) {
                    claimed = claimed.add(new BigDecimal(claim.amount()));
                }
            }
            for (Claim claim : mine) {
                BigDecimal d = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
                // The holders' parts of a shared claim count for them, not for its own person.
                BigDecimal own = d == null ? null : d.subtract(claim.sharedPart(d));
                due = own == null ? due : due.add(own);
                if (own != null && claim.status() == ClaimStatus.SETTLED && !claim.correction()) {
                    settled = settled.add(own);
                }
                if (late.overdue(claim, person.gradeOn(claim.date()))) {
                    overdueCount++;
                    if (d != null) {
                        overdue = overdue.add(d);
                    }
                }
            }
            // The person's parts of the claims shared with them, counted as their own persons count them.
            boolean holdsPart = false;
            for (Claim claim : all) {
                Person owner = byId.get(claim.person());
                if (owner == null || !owner.counts(claim)) {
                    continue;
                }
                for (var share : claim.shares()) {
                    if (share.person() != person.id()) {
                        continue;
                    }
                    BigDecimal d = claim.due(sheetRates, owner.gradeOn(claim.date()), floor, late);
                    if (d == null) {
                        continue;
                    }
                    BigDecimal part = share.partOf(d);
                    holdsPart = true;
                    due = due.add(part);
                    if (claim.status() == ClaimStatus.SETTLED && !claim.correction()) {
                        settled = settled.add(part);
                    }
                }
            }
            // n/a when the grade has no rate on the sheet's today, unless a claim has a figure apart from it, or they
            // hold a part of a claim shared with them.
            boolean na = !holdsPart && Rate.on(sheetRates, person.grade(), today) == null
                    && mine.stream().noneMatch((c) -> person.figureApartFrom(c, today, sheetRates, floor));
            due = Settings.capped(due, cap);
            claimedTotal = claimedTotal.add(claimed);
            overdueFigures.add(overdue);
            if (!na) {
                dueFigures.add(due);
            }
            figures.put(person.id(), new Figures(person.name(), claimed.toPlainString(), na ? "n/a" : due.toPlainString(),
                    overdue.toPlainString(), score(settled, overdueCount)));
        }
        return new Snapshot(figures, claimedTotal.toPlainString(), trimmedSum(dueFigures).toPlainString(),
                trimmedSum(overdueFigures).toPlainString());
    }

    /** A person's score: the settled figure of their SETTLED claims that correct nothing, divided by 100, minus 5 for every overdue claim of theirs that counts,
     *  rounded half to even to a whole number. */
    static long score(BigDecimal settled, int overdueCount) {
        return settled.movePointLeft(2).subtract(BigDecimal.valueOf(5L * overdueCount))
                .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /** Adds up the people's figures, leaving out the single highest and the single lowest when three or more have one. */
    static BigDecimal trimmedSum(List<BigDecimal> figures) {
        BigDecimal sum = figures.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (figures.size() < 3) {
            return sum;
        }
        return sum.subtract(Collections.max(figures)).subtract(Collections.min(figures));
    }

    /** Audits the figures the change moved since {@code before}, after the change's own lines. */
    public void record(String sheet, String user, Snapshot before) {
        Snapshot after = take(sheet);
        String prefix = " sheet=" + sheet + " by=" + user + " ";
        for (Map.Entry<Long, Figures> entry : after.people().entrySet()) {
            Figures now = entry.getValue();
            Figures was = before.people().get(entry.getKey());
            if (was == null || !was.claimed().equals(now.claimed()) || !was.dueTotal().equals(now.dueTotal())
                    || !was.overdueTotal().equals(now.overdueTotal())) {
                audit.record("ROLLUP" + prefix + "person=" + now.name() + " claimed=" + now.claimed() + " due-total="
                        + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        }
        if (!Objects.equals(before.claimedTotal(), after.claimedTotal())
                || !Objects.equals(before.dueTotal(), after.dueTotal())
                || !Objects.equals(before.overdueTotal(), after.overdueTotal())) {
            audit.record("TOTALS" + prefix + "claimed-total=" + after.claimedTotal() + " due-total=" + after.dueTotal()
                    + " overdue-total=" + after.overdueTotal());
        }
        BigDecimal threshold = settings.alertThreshold(sheet);
        if (threshold == null) {
            return;
        }
        for (Map.Entry<Long, Figures> entry : after.people().entrySet()) {
            Figures now = entry.getValue();
            Figures was = before.people().get(entry.getKey());
            if (above(now, threshold) && (was == null || !above(was, threshold))) {
                audit.record("ALERT" + prefix + "name=" + now.name() + " due-total=" + now.dueTotal());
            }
        }
    }

    /** Whether the person has a due-total above the threshold. */
    private static boolean above(Figures figures, BigDecimal threshold) {
        return !"n/a".equals(figures.dueTotal()) && new BigDecimal(figures.dueTotal()).compareTo(threshold) > 0;
    }
}

package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogStore;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.claim.Lateness;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.claim.Shares;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.stereotype.Service;

/**
 * Works out the sheet's figures as the screen shows them, and records in the audit file how a change
 * moved them: ROLLUP sheet=<id> by=<user> person=<name> claimed=<0.00> due-total=<0.00 or n/a>
 * overdue-total=<0.00> for each person whose figures changed (a person new to the sheet counts as changed), in
 * list order (by their positions), then TOTALS sheet=<id> by=<user> claimed-total=<0.00> due-total=<0.00>
 * overdue-total=<0.00> when any of the sheet's totals changed; the sheet's due-total and overdue-total leave out
 * the single highest and the single lowest person's figure when three or more people have one (n/a is no figure). A person's overdue-total is the due (late charge
 * included, skipping n/a) of their counting claims that are OPEN or PENDING with a due-by (plus any grace days of their grade, see {@link Lateness}) before today. A person's due-total is at most the sheet's cap less their cut total (see {@link PoolCuts#totals}), not below 0.00; a shared claim's
 * holders' parts (see {@link Shares}: at their own grade's rate, frozen once settled) count in their due-total and settled figures, not in its person's. A change that moves no figure records nothing here.
 * After those lines, while the sheet's alert threshold is set, ALERT sheet=<id> by=<user> name=<name> due-total=<0.00>
 * for each person whose due-total the change took from at or below it (or no due-total) to above it, in list order.
 */
@Service
public class RollupService {

    private static final String NA = "n/a";

    private final PersonStore people;
    private final ClaimStore claims;
    private final RateStore rates;
    private final SheetStore sheets;
    private final SettingStore settings;
    private final Audit audit;
    private final ClaimLogStore log;
    private final PoolCuts cuts;

    public RollupService(PersonStore people, ClaimStore claims, RateStore rates, SheetStore sheets, SettingStore settings,
            Audit audit, ClaimLogStore log, PoolCuts cuts) {
        this.cuts = cuts;
        this.people = people;
        this.log = log;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures as the store holds them now. */
    public Rollup snapshot(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        BigDecimal cap = settings.cap(sheetId);
        Map<Long, BigDecimal> cutTotals = cap == null ? Map.of() : cuts.totals(sheetId);
        Lateness lateness = lateness(sheetId, today, all);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        // The parts of the shared claims that count: each claim's parts added up, and each holder's.
        Map<Long, BigDecimal> shared = new HashMap<>();
        Map<Long, BigDecimal> heldDue = new HashMap<>();
        Map<Long, BigDecimal> heldSettled = new HashMap<>();
        Set<Long> heldFixed = new HashSet<>();
        Map<Long, Person> byId = new HashMap<>();
        people.list(sheetId).forEach(p -> byId.put(p.getId(), p));
        for (Claim claim : all) {
            Person owner = byId.get(claim.getPersonId());
            List<Shares.Share> holders = Shares.list(claim.getShares());
            if (holders.isEmpty() || owner == null || !countsClaimed(claim, owner, today) || !counts(claim, owner, today)) {
                continue;
            }
            BigDecimal figure = lateness.due(claim, rates.forClaim(claim).find(sheetId, owner.gradeOn(claim.getDate()), claim.getDate()));
            if (figure == null) {
                continue;
            }
            for (Shares.Share holder : holders) {
                Person held = byId.get(holder.personId());
                String grade = held == null ? null : held.gradeOn(claim.getDate());
                BigDecimal pct = grade == null ? null : rates.forClaim(claim).find(sheetId, grade, claim.getDate());
                BigDecimal part = Shares.part(claim, figure, holder.share(), pct, lateness.rounding());
                // A frozen part, or one at a grade other than the holder's today or at a rate that ended before
                // today, does not hang on today's grade's rate.
                if (claim.getStatus() == ClaimStatus.SETTLED || (pct != null && (!grade.equals(held.gradeOn(today))
                        || rates.forClaim(claim).endsBefore(sheetId, grade, claim.getDate(), today)))) {
                    heldFixed.add(holder.personId());
                }
                shared.merge(claim.getId(), part, BigDecimal::add);
                heldDue.merge(holder.personId(), part, BigDecimal::add);
                if (claim.getStatus() == ClaimStatus.SETTLED
                        && (claim.getCorrects() == null || claim.getCorrects().isBlank())) {
                    heldSettled.merge(holder.personId(), part, BigDecimal::add);
                }
            }
        }
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            BigDecimal settled = BigDecimal.ZERO.setScale(2);
            int overdueCount = 0;
            boolean fixed = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId()
                        || !countsClaimed(claim, person, today)) {
                    continue;
                }
                // A claim that corrects another counts toward due but not toward claimed.
                if (claim.getCorrects() == null || claim.getCorrects().isBlank()) {
                    claimed = claimed.add(claim.getAmount());
                }
                // A held claim stays in claimed, but leaves the due figures while held.
                if (!counts(claim, person, today)) {
                    continue;
                }
                BigDecimal figure = lateness.due(claim, rates.forClaim(claim).find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()));
                if (lateness.overdue(claim)) {
                    overdueCount++;
                }
                if (figure != null) {
                    // The holders' parts of a shared claim are theirs, not its person's.
                    BigDecimal own = figure.subtract(shared.getOrDefault(claim.getId(), BigDecimal.ZERO));
                    due = due.add(own);
                    if (lateness.overdue(claim)) {
                        overdue = overdue.add(figure);
                    }
                    // The score's settled figures leave out corrections.
                    if (claim.getStatus() == ClaimStatus.SETTLED
                            && (claim.getCorrects() == null || claim.getCorrects().isBlank())) {
                        settled = settled.add(own);
                    }
                }
                // A figure at a grade other than the one in force today, or at a rate that ended before
                // today, does not hang on today's grade's rate.
                fixed |= claim.getSettledDue() != null || (claim.isBound() && claim.getStatedDue() != null)
                        || (figure != null && (!person.gradeOn(claim.getDate()).equals(person.gradeOn(today))
                                || rates.forClaim(claim).endsBefore(sheetId, person.gradeOn(claim.getDate()), claim.getDate(), today)));
            }
            // Parts of claims shared with them follow their own grade's rate on the claims' dates (frozen once settled).
            if (heldDue.containsKey(person.getId())) {
                due = due.add(heldDue.get(person.getId()));
                settled = settled.add(heldSettled.getOrDefault(person.getId(), BigDecimal.ZERO));
                fixed |= heldFixed.contains(person.getId());
            }
            boolean na = rates.find(sheetId, person.gradeOn(today), today) == null && !fixed;
            claimedTotal = claimedTotal.add(claimed);
            overdues.add(overdue);
            due = SettingStore.capped(due, SettingStore.capLess(cap, cutTotals.get(person.getId())));
            if (!na) {
                dues.add(due);
            }
            figures.put(person.getId(), new Rollup.Figures(person.getName(), shown(claimed), na ? NA : shown(due),
                    shown(overdue), score(na ? BigDecimal.ZERO : settled, overdueCount)));
        }
        return new Rollup(figures, shown(claimedTotal), shown(trimmed(dues)), shown(trimmed(overdues)));
    }

    /** The sum of people's figures, leaving out the single highest and the single lowest when three or more
     *  people have one. */
    private static BigDecimal trimmed(List<BigDecimal> figures) {
        List<BigDecimal> sorted = new ArrayList<>(figures);
        sorted.sort(null);
        if (sorted.size() >= 3) {
            sorted = sorted.subList(1, sorted.size() - 1);
        }
        return sorted.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** Each person's due-total of their claims due by a date in from..to (inclusive) that count by the status they
     *  had at the end of {@code to} (see {@link #countsAt}), by person id in the order they were added: the
     *  sum of their figures as shown now, skipping n/a, as the screen shows a person's due-total for a
     *  period. A period's due-total is the sum of these. */
    public Map<Long, BigDecimal> dueBetweenByPerson(String sheetId, LocalDate from, LocalDate to) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        Lateness lateness = lateness(sheetId, today, all);
        Map<Long, ClaimStatus> atEnd = log.statusesAt(sheetId, to);
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId()
                        || !countsAt(claim, atEnd.get(claim.getId()), person, today)
                        || claim.getDueBy().isBefore(from) || claim.getDueBy().isAfter(to)) {
                    continue;
                }
                BigDecimal figure = lateness.due(claim, rates.forClaim(claim).find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()));
                if (figure != null) {
                    total = total.add(figure);
                }
            }
            totals.put(person.getId(), total);
        }
        return totals;
    }

    /** Each person's figure carried into a period ending on {@code to}, by person id in the order they were added:
     *  the sum of the figures (as shown now, skipping n/a) of their claims due by a date in one of the
     *  {@code earlier} periods (each from..to, inclusive) that were OPEN or PENDING at the end of {@code to} and
     *  count then (see {@link #countsAt}), as the screen shows a person's carried figure for an open period. */
    public Map<Long, BigDecimal> carriedByPerson(String sheetId, LocalDate to, List<LocalDate[]> earlier) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        Lateness lateness = lateness(sheetId, today, all);
        Map<Long, ClaimStatus> atEnd = log.statusesAt(sheetId, to);
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Claim claim : all) {
                ClaimStatus status = atEnd.get(claim.getId());
                if (claim.getPersonId() != person.getId() || status == null || !status.isOpen()
                        || !countsAt(claim, status, person, today)
                        || earlier.stream().noneMatch(p -> !claim.getDueBy().isBefore(p[0]) && !claim.getDueBy().isAfter(p[1]))) {
                    continue;
                }
                BigDecimal figure = lateness.due(claim, rates.forClaim(claim).find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()));
                if (figure != null) {
                    total = total.add(figure);
                }
            }
            totals.put(person.getId(), total);
        }
        return totals;
    }

    /** Records how the sheet's figures moved since {@code before}, the change made by {@code user}. */
    public void record(String sheetId, String user, Rollup before) {
        Rollup after = snapshot(sheetId);
        String who = " sheet=" + sheetId + " by=" + user;
        after.people().forEach((id, now) -> {
            if (!now.sameAs(before.people().get(id))) {
                audit.record("ROLLUP" + who + " person=" + now.name() + " claimed=" + now.claimed() + " due-total="
                        + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        });
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())
                || !after.overdueTotal().equals(before.overdueTotal())) {
            audit.record("TOTALS" + who + " claimed-total=" + after.claimedTotal() + " due-total=" + after.dueTotal()
                    + " overdue-total=" + after.overdueTotal());
        }
        alerts(sheetId, who, before, after);
    }

    /** Records ALERT for each person whose due-total went from at or below the sheet's alert threshold (or none)
     *  to above it, in list order; nothing while the threshold is off. */
    private void alerts(String sheetId, String who, Rollup before, Rollup after) {
        BigDecimal threshold = settings.alertThreshold(sheetId);
        if (threshold == null) {
            return;
        }
        after.people().forEach((id, now) -> {
            Rollup.Figures was = before == null ? null : before.people().get(id);
            if (above(now, threshold) && !above(was, threshold)) {
                audit.record("ALERT" + who + " name=" + now.name() + " due-total=" + now.dueTotal());
            }
        });
    }

    private static boolean above(Rollup.Figures figures, BigDecimal threshold) {
        return figures != null && !NA.equals(figures.dueTotal())
                && new BigDecimal(figures.dueTotal()).compareTo(threshold) > 0;
    }

    /** Records the sheet's figures as they stand, the change made by {@code user}: a ROLLUP line for every
     *  person, then its TOTALS, then an ALERT for each person above the alert threshold (each counts as new). */
    public void recordAll(String sheetId, String user) {
        Rollup now = snapshot(sheetId);
        String who = " sheet=" + sheetId + " by=" + user;
        now.people().values().forEach(p -> audit.record("ROLLUP" + who + " person=" + p.name() + " claimed="
                + p.claimed() + " due-total=" + p.dueTotal() + " overdue-total=" + p.overdueTotal()));
        audit.record("TOTALS" + who + " claimed-total=" + now.claimedTotal() + " due-total=" + now.dueTotal()
                + " overdue-total=" + now.overdueTotal());
        alerts(sheetId, who, null, now);
    }

    /** What the sheet's late charge follows on the date, for these claims (see {@link Lateness}). */
    private Lateness lateness(String sheetId, LocalDate today, List<Claim> all) {
        Map<Long, Person> byId = new HashMap<>();
        people.list(sheetId).forEach(person -> byId.put(person.getId(), person));
        return new Lateness(settings.latePcts(sheetId), settings.floors(sheetId), today,
                new Rounding(policyDate(sheetId), settings.roundings(sheetId), all), Lateness.graced(settings.graces(sheetId), byId::get));
    }

    /** The date the sheet's rounding turns on: the policy date set, else the sheet's today. */
    private LocalDate policyDate(String sheetId) {
        LocalDate policy = settings.policyDate(sheetId);
        return policy != null ? policy : sheets.today(sheetId);
    }

    /** Only OPEN, PENDING and SETTLED claims count, and an OPEN or PENDING claim of a person whose retirement
     *  date the sheet's today has reached does not, unless a statement bound it; nor does a claim deferred
     *  on the sheet's today (see {@link Claim#isDeferredOn}: a bound claim is not, until its binding ends). A hold (the claim's or its person's) does not take it out: holds only touch cuts. */
    public static boolean counts(Claim claim, Person person, LocalDate today) {
        return !claim.isDeferredOn(today) && (claim.getStatus() == ClaimStatus.SETTLED
                || (claim.getStatus().isOpen() && (claim.isStated() || !person.retiredOn(today))));
    }

    /** Whether the claim counts toward claimed: as {@link #counts}. */
    public static boolean countsClaimed(Claim claim, Person person, LocalDate today) {
        return !claim.isDeferredOn(today) && (claim.getStatus() == ClaimStatus.SETTLED
                || (claim.getStatus().isOpen() && (claim.isStated() || !person.retiredOn(today))));
    }

    /** Whether the claim counts by {@code status}, the status it had at a period's end (null when it
     *  was added after it): OPEN, PENDING or SETTLED then; and, as the live rules say, an OPEN claim
     *  deferred on the sheet's today does not, nor one open then and still open now of a person retired on
     *  the sheet's today, unless a statement bound it. */
    static boolean countsAt(Claim claim, ClaimStatus status, Person person, LocalDate today) {
        return !claim.isDeferredOn(today) && (status == ClaimStatus.SETTLED
                || (status != null && status.isOpen()
                        && (claim.isStated() || !claim.getStatus().isOpen()
                                || !person.retiredOn(today))));
    }

    /** A person's score: their settled-total leaving out corrections (0 while their due-total is n/a) divided by
     *  100, less 5 for every overdue claim of theirs that counts, rounded half to even to a whole number. */
    private static String score(BigDecimal settled, int overdueCount) {
        return settled.movePointLeft(2).subtract(BigDecimal.valueOf(5L * overdueCount))
                .setScale(0, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static String shown(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

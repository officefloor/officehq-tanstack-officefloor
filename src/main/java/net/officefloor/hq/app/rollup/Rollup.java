package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSetting;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's figures at one moment, as the screen shows them: each person's claimed (corrections
 *  left out; held ones count, see {@link Person#countsClaimed}) and
 *  due-total (null: n/a; else at most the sheet's cap less their cut total, not below 0.00) and overdue-total, in the order they were added, and the
 *  sheet's claimed-total, due-total and overdue-total (these two leave out the single highest and
 *  lowest person's figure when three or more people have one), with the sheet's alert threshold
 *  (null: off). A shared claim's due counts toward each holder's due-total (and settled figures) by their part
 *  (see {@link ClaimShare#partOf}), its own person's by the rest. */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal, String overdueTotal,
        BigDecimal alertThreshold) {

    /** A person's figures: their name, claimed, due-total (null: n/a) and overdue-total (the shown
     *  dues of their open claims past their due-by plus grace days on the sheet's today, n/a ones skipped) and
     *  score (their settled claims' shown dues, corrections left out, divided by 100, less 5 per overdue claim, rounded half to even;
     *  not part of {@link #sameAs}). */
    public record Figures(String name, String claimed, String dueTotal, String overdueTotal, String score) {
        public boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed)
                    && (dueTotal == null ? other.dueTotal == null : dueTotal.equals(other.dueTotal))
                    && overdueTotal.equals(other.overdueTotal);
        }
    }

    /** The sheet's figures now. */
    public static Rollup of(String sheetId, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SheetRepository sheets, SettingRepository settings, PoolRepository pools) {
        List<Rate> sheetRates = rates.list(sheetId);
        DatedSetting floor = settings.floor(sheetId);
        BigDecimal cap = settings.cap(sheetId);
        Map<Long, BigDecimal> cuts = cap == null ? Map.of() : pools.cutTotals(sheetId);
        LocalDate todayDate = sheets.today(sheetId);
        String today = todayDate.toString();
        Dues.Late late = settings.late(sheetId, todayDate);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = zero();
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        List<Person> listed = people.list(sheetId);
        // The parts of shared claims' dues each holder holds (and of those, of SETTLED claims, corrections left out).
        Map<Long, BigDecimal> heldDue = new java.util.HashMap<>();
        Map<Long, BigDecimal> heldSettled = new java.util.HashMap<>();
        Map<Long, BigDecimal[]> own = new LinkedHashMap<>();
        Map<Long, int[]> overdueCounts = new java.util.HashMap<>();
        Map<Long, Boolean> keeps = new java.util.HashMap<>();
        for (Person p : listed) {
            BigDecimal claimed = zero();
            BigDecimal due = zero();
            BigDecimal overdue = zero();
            BigDecimal settled = zero();
            int overdueCount = 0;
            boolean kept = false;
            for (Claim c : all) {
                if (c.getPersonId() != p.getId()) continue;
                if (!corrects(c) && p.countsClaimed(c)) claimed = claimed.add(new BigDecimal(c.getAmount()));
                if (!p.countsLive(c)) continue;
                String grade = p.gradeOn(c.getDate());
                Rate rate = RateRepository.inForce(sheetRates, grade, c.getDate(), c.getId());
                String pct = rate == null ? null : rate.pct();
                String d = Dues.shown(c, pct == null ? null : Dues.of(c.getAmount(), pct, late.rounding(c.getRoundDate())), floor.on(c.getDate()), late);
                kept |= c.getSettledDue() != null || c.isBound()
                        || (pct != null && (!grade.equals(p.getGrade()) || c.getDate().compareTo(today) <= 0));
                boolean settles = !corrects(c) && ClaimStatus.SETTLED.name().equals(c.getStatus());
                if (d != null) {
                    BigDecimal full = new BigDecimal(d);
                    BigDecimal net = full;
                    for (ClaimShare share : c.getShares()) {
                        BigDecimal part = share.partOf(full);
                        net = net.subtract(part);
                        heldDue.merge(share.personId(), part, BigDecimal::add);
                        if (settles) heldSettled.merge(share.personId(), part, BigDecimal::add);
                    }
                    due = due.add(net);
                    if (settles) settled = settled.add(net);
                }
                if (ClaimStatus.isOpen(c.getStatus()) && late.lateFrom(c).isBefore(todayDate)) {
                    overdueCount++;
                    if (d != null) overdue = overdue.add(new BigDecimal(d));
                }
            }
            own.put(p.getId(), new BigDecimal[] { claimed, due, overdue, settled });
            overdueCounts.put(p.getId(), new int[] { overdueCount });
            keeps.put(p.getId(), kept);
        }
        for (Person p : listed) {
            BigDecimal[] f = own.get(p.getId());
            BigDecimal claimed = f[0];
            BigDecimal held = heldDue.get(p.getId());
            BigDecimal due = held == null ? f[1] : f[1].add(held);
            BigDecimal overdue = f[2];
            BigDecimal settled = f[3].add(heldSettled.getOrDefault(p.getId(), zero()));
            boolean kept = keeps.get(p.getId()) || held != null;
            boolean na = rateOn(sheetRates, p.getGrade(), today) == null && !kept;
            if (cap != null) {
                BigDecimal room = capLessCuts(cap, cuts.get(p.getId()));
                if (due.compareTo(room) > 0) due = room;
            }
            figures.put(p.getId(), new Figures(p.getName(), claimed.toPlainString(),
                    na ? null : due.toPlainString(), overdue.toPlainString(),
                    score(settled, overdueCounts.get(p.getId())[0])));
            claimedTotal = claimedTotal.add(claimed);
            if (!na) dues.add(due);
            overdues.add(overdue);
        }
        return new Rollup(figures, claimedTotal.toPlainString(), trimmedSum(dues).toPlainString(),
                trimmedSum(overdues).toPlainString(), settings.alertThreshold(sheetId));
    }

    /** The audit lines after a change from {@code before} to this: one ROLLUP per person whose
     *  figures moved (a new person counts as moved), then TOTALS when a sheet total moved, then an
     *  ALERT per person (in list order) whose due-total went from at or below the alert threshold
     *  (or none) to above it. */
    public List<String> changesSince(Rollup before, String sheetId, String by) {
        String head = " sheet=" + sheetId + " by=" + by;
        List<String> lines = new ArrayList<>();
        people.forEach((id, now) -> {
            if (!now.sameAs(before.people.get(id))) {
                lines.add("ROLLUP" + head + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + (now.dueTotal() == null ? "n/a" : now.dueTotal())
                        + " overdue-total=" + now.overdueTotal());
            }
        });
        if (!claimedTotal.equals(before.claimedTotal) || !dueTotal.equals(before.dueTotal)
                || !overdueTotal.equals(before.overdueTotal)) {
            lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal
                    + " overdue-total=" + overdueTotal);
        }
        if (alertThreshold != null) {
            people.forEach((id, now) -> {
                Figures was = before.people.get(id);
                if (above(now) && (was == null || !above(was))) {
                    lines.add("ALERT" + head + " name=" + now.name() + " due-total=" + now.dueTotal());
                }
            });
        }
        return lines;
    }

    /** Whether the person's due-total is above the alert threshold (none: not). */
    private boolean above(Figures figures) {
        return figures.dueTotal() != null && new BigDecimal(figures.dueTotal()).compareTo(alertThreshold) > 0;
    }

    /** What a person's due-total is held to: the cap less their cut total (none: 0.00), not below 0.00. */
    public static BigDecimal capLessCuts(BigDecimal cap, BigDecimal cutTotal) {
        BigDecimal room = cutTotal == null ? cap : cap.subtract(cutTotal);
        return room.signum() < 0 ? zero() : room.setScale(2);
    }

    /** Whether the claim corrects another: such a claim is left out of claimed. */
    private static boolean corrects(Claim c) {
        return c.getCorrects() != null && !c.getCorrects().isBlank();
    }

    /** The grade's rate in force on the date (yyyy-MM-dd), inherited up the parent chain; null when none. */
    private static String rateOn(List<Rate> rates, String grade, String date) {
        Rate found = RateRepository.inForce(rates, grade, date);
        return found == null ? null : found.pct();
    }

    /** The figures added up, leaving out the single highest and the single lowest when there are three or more. */
    private static BigDecimal trimmedSum(List<BigDecimal> figures) {
        List<BigDecimal> sorted = new ArrayList<>(figures);
        sorted.sort(BigDecimal::compareTo);
        if (sorted.size() >= 3) sorted = sorted.subList(1, sorted.size() - 1);
        BigDecimal total = zero();
        for (BigDecimal f : sorted) total = total.add(f);
        return total;
    }

    /** A person's score: their settled figures divided by 100, less 5 per overdue claim, rounded half to even. */
    private static String score(BigDecimal settled, int overdueCount) {
        return settled.movePointLeft(2).subtract(BigDecimal.valueOf(5L * overdueCount))
                .setScale(0, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}

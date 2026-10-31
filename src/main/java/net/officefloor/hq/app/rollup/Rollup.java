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
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them — each person's claimed (leaving out corrections; a hold does not take a claim out) and due-total, and the
 * sheet's claimed-total and due-total, each person's and the sheet's overdue-total (the dues of their overdue claims, uncapped), each person's due-total held to the sheet's cap — worked out on the server so a change can audit what it
 * moved: after the change's own lines, one ROLLUP line per person whose figures changed (people in
 * the order they were added; a new person counts as changed), then a TOTALS line when the sheet's
 * totals changed. The sheet's due-total and overdue-total leave out the single highest and the single
 * lowest person's figure when three or more people have one (n/a is no figure). While the sheet has an
 * alert threshold, after those lines one ALERT line per person (in list order) whose due-total went from
 * at or below it (or none) to above it.
 */
@Service
public class Rollup {

    private static final String NA = "n/a";

    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final Sheets sheets;
    private final Settings settings;
    private final Audit audit;

    public Rollup(People people, Claims claims, Rates rates, Sheets sheets, Settings settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures now. */
    public RollupFigures figures(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        LocalDate policyDate = settings.policyDate(sheetId);
        BigDecimal cap = settings.cap(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, RollupFigures.PersonFigures> byPerson = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            BigDecimal settled = BigDecimal.ZERO.setScale(2);
            int overdueCount = 0;
            boolean anyFigure = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !person.countsClaimed(claim, today)) {
                    continue;
                }
                if (claim.getCorrects() == null || claim.getCorrects().isEmpty()) {
                    claimed = claimed.add(claim.getAmount());
                }
                if (!person.countsLive(claim, today)) {
                    continue;
                }
                BigDecimal figure = due(claim, person, today, policyDate);
                if (isOverdue(claim, grace(claim, person), today)) {
                    overdueCount++;
                }
                if (figure != null && claim.getStatus() == net.officefloor.hq.app.claim.ClaimStatus.SETTLED) {
                    settled = settled.add(figure);
                }
                if (figure != null) {
                    anyFigure = true;
                    due = due.add(figure);
                    if (isOverdue(claim, grace(claim, person), today)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            // n/a only when no claim keeps or gets a figure (each at the grade in force on its date).
            boolean na = !anyFigure && rates.find(sheetId, person.getGrade(), today) == null;
            due = Settings.capped(due, cap);
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dues.add(due);
            }
            overdues.add(overdue);
            byPerson.put(person.getId(), new RollupFigures.PersonFigures(person.getName(),
                    show(claimed), na ? NA : show(due), show(overdue),
                    score(settled, overdueCount)));
        }
        return new RollupFigures(byPerson, show(claimedTotal), show(trimmedSum(dues)), show(trimmedSum(overdues)));
    }

    /** A claim's due as the screen shows it now; null for n/a. */
    public BigDecimal due(Claim claim, Person person) {
        String sheetId = claim.getSheetId();
        return due(claim, person, sheets.today(sheetId), settings.policyDate(sheetId));
    }

    /** A claim's due: fixed when settled; else the figure a statement bound it to, or its amount at the rate of its person's grade
     *  in force on its date, with its late charge (each 30-day boundary at the late percent in force on it) as of today on top,
     *  raised to the floor in force on its date. */
    private BigDecimal due(Claim claim, Person person, LocalDate today, LocalDate policyDate) {
        String sheetId = claim.getSheetId();
        RoundingMode rounding = Rounding.of(claim, policyDate, on -> settings.roundingMode(sheetId, on));
        BigDecimal fixed = ClaimResponse.fixedDue(claim);
        BigDecimal figure = fixed != null || claim.isBound() ? fixed
                : Rates.due(claim.getAmount(), rates.find(claim, person.gradeOn(claim.getDate())), rounding);
        return LateCharge.due(claim, figure, settings.floor(sheetId, claim.getDate()), on -> settings.latePct(sheetId, on), today, rounding,
                grace(claim, person));
    }

    /** Audit (on commit) one SNAPSHOT line per person, in list order, with their due-total in {@code before}, and keep
     *  it on the person dated the sheet's today (so call it before today moves). */
    public void recordSnapshots(String sheetId, String user, RollupFigures before, LocalDate takenOn) {
        String prefix = " sheet=" + sheetId + " by=" + user;
        before.people().forEach((id, was) -> {
            people.recordSnapshot(sheetId, id, was.dueTotal(), takenOn);
            audit.recordOnCommit("SNAPSHOT" + prefix + " name=" + was.name() + " due-total=" + was.dueTotal());
        });
    }

    /** Audit (on commit) the figures that moved since {@code before}, as taken by {@code user}. */
    public void recordChanges(String sheetId, String user, RollupFigures before) {
        RollupFigures after = figures(sheetId);
        String prefix = " sheet=" + sheetId + " by=" + user;
        after.people().forEach((id, now) -> {
            RollupFigures.PersonFigures was = before.people().get(id);
            if (was == null || !Objects.equals(was.claimed(), now.claimed())
                    || !Objects.equals(was.dueTotal(), now.dueTotal())
                    || !Objects.equals(was.overdueTotal(), now.overdueTotal())) {
                audit.recordOnCommit("ROLLUP" + prefix + " person=" + now.name()
                        + " claimed=" + now.claimed() + " due-total=" + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        });
        if (!before.claimedTotal().equals(after.claimedTotal()) || !before.dueTotal().equals(after.dueTotal())
                || !before.overdueTotal().equals(after.overdueTotal())) {
            audit.recordOnCommit("TOTALS" + prefix + " claimed-total=" + after.claimedTotal()
                    + " due-total=" + after.dueTotal() + " overdue-total=" + after.overdueTotal());
        }
        BigDecimal threshold = settings.alertThreshold(sheetId);
        if (threshold != null) {
            after.people().forEach((id, now) -> {
                RollupFigures.PersonFigures was = before.people().get(id);
                if (above(now.dueTotal(), threshold) && (was == null || !above(was.dueTotal(), threshold))) {
                    audit.recordOnCommit("ALERT" + prefix + " name=" + now.name() + " due-total=" + now.dueTotal());
                }
            });
        }
    }

    /** A person's score: their settled-total over 100, minus 5 for each of their overdue counting claims, rounded half to
     *  even to a whole number. */
    private static String score(BigDecimal settled, int overdueCount) {
        return settled.subtract(BigDecimal.valueOf(5L * overdueCount * 100)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_EVEN)
                .toPlainString();
    }

    /** Audit (on commit), while the sheet's score log is on, one SCORE line per person, in list order, with their score in
     *  {@code before} (so call it before the SNAPSHOT lines). */
    public void recordScores(String sheetId, String user, RollupFigures before) {
        if (!settings.scoreLog(sheetId)) {
            return;
        }
        String prefix = " sheet=" + sheetId + " by=" + user;
        before.people().forEach((id, was) -> audit.recordOnCommit("SCORE" + prefix + " name=" + was.name() + " score=" + was.score()));
    }

    /** Whether a due-total as shown is above the threshold (n/a is no due-total). */
    private static boolean above(String dueTotal, BigDecimal threshold) {
        return !NA.equals(dueTotal) && new BigDecimal(dueTotal).compareTo(threshold) > 0;
    }

    /** The grace days of the claim's person's grade in force on its date, as in force on that date (0 for none). */
    private int grace(Claim claim, Person person) {
        return settings.grace(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
    }

    /** A claim is overdue when it is OPEN (or PENDING) and its due-by plus its grace days is before the sheet's today. */
    private static boolean isOverdue(Claim claim, int grace, LocalDate today) {
        return claim.getStatus().isOpen() && claim.getDueBy() != null && LateCharge.lateFrom(claim, grace).isBefore(today);
    }

    /** People's figures added up, leaving out the single highest and the single lowest when there are three or more. */
    private static BigDecimal trimmedSum(List<BigDecimal> figures) {
        BigDecimal sum = figures.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (figures.size() < 3) {
            return sum;
        }
        return sum.subtract(figures.stream().max(BigDecimal::compareTo).orElseThrow())
                .subtract(figures.stream().min(BigDecimal::compareTo).orElseThrow());
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

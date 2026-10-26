package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures, as the screen shows them: a person's claimed is their counting claims' amounts added up;
 * their due-total is those claims' figures added up (a claim's figure: its settled due, else the figure a statement
 * bound it to, else its amount times the rate in force on its date of the grade in force on its date (the sheet's own, else its parent's, up to three levels up), rounded half up when the claim rounds by a date before the sheet's policy date, else half to even;
 * none without a rate) with its late charge added on top while it is overdue, and then — unless it is settled or a correction — at least the sheet's floor in force on its date, n/a when their grade on the sheet's today has no rate then and no counting claim keeps a settled or bound
 * figure or one at another grade, and at most the sheet's cap in force on its today (an n/a one is not capped); their overdue-total is the figures (late charge included) of
 * those of their claims that are OPEN or PENDING and past their due-by plus their grade's grace on the sheet's today, added up (uncapped). After a change, records one ROLLUP line per person whose figures (overdue-total included) it moved,
 * then a TOTALS line when any of the sheet's three totals moved. The sheet's due-total and overdue-total leave out the
 * single highest and the single lowest person's figure when three or more people have one (an n/a due-total is none). A claim deferred past the sheet's today counts in none of these. A person's OPEN and PENDING claims dated within one of their holds count in none of these, unless a statement
 * binds them; their other claims still do. So too, from the day their retirement date is reached (the sheet's today on or
 * after it), do none of a retired person's OPEN and PENDING claims, unless a statement binds them.
 */
@Service
public class Rollup {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;
    private final ClaimLogRepository log;
    private final Audit audit;

    public Rollup(PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, ClaimLogRepository log, Audit audit) {
        this.log = log;
        this.settings = settings;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.audit = audit;
    }

    public RollupSnapshot snapshot(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Rate> sheetRates = rates.chain(sheetId);
        List<Claim> sheetClaims = claims.list(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        BigDecimal cap = late.settings().cap(today);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        for (Person p : people.list(sheetId)) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            BigDecimal overdue = ZERO;
            boolean kept = false;
            for (Claim c : sheetClaims) {
                if (c.getPersonId() != p.getId() || !counts(c, p, today)) {
                    continue;
                }
                claimed = claimed.add(c.getAmount().setScale(2));
                kept |= c.getSettledDue() != null || (c.isBound() && c.getStatedDue() != null);
                String grade = p.gradeOn(c.getDate());
                BigDecimal figure = late.due(c, figure(c, grade, sheetRates, late));
                // A figure at a grade other than today's does not hang on today's grade having a rate; nor does one
                // dated on or before today, whose rate may have ended since.
                kept |= figure != null && (!grade.equals(p.getGrade()) || !c.getDate().isAfter(today));
                if (figure != null) {
                    due = due.add(figure);
                    if (late.overdue(c)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            BigDecimal personDue = SettingRepository.capped(
                    kept || rateOn(sheetRates, p.getGrade(), today) != null ? due : null, cap);
            figures.put(p.getId(), new Figures(p.getName(), claimed, personDue, overdue));
            claimedTotal = claimedTotal.add(claimed);
            overdues.add(overdue);
            if (personDue != null) {
                dues.add(personDue);
            }
        }
        return new RollupSnapshot(figures, claimedTotal, trimmed(dues), trimmed(overdues));
    }

    /** The figures added up, leaving out the single highest and the single lowest when there are three or more. */
    static BigDecimal trimmed(List<BigDecimal> figures) {
        BigDecimal sum = figures.stream().reduce(ZERO, BigDecimal::add);
        if (figures.size() < 3) {
            return sum;
        }
        BigDecimal max = figures.stream().max(BigDecimal::compareTo).orElseThrow();
        BigDecimal min = figures.stream().min(BigDecimal::compareTo).orElseThrow();
        return sum.subtract(max).subtract(min);
    }

    /**
     * The due-total of the claims dated from..to (inclusive) counting by their status at the end of {@code to}, as the
     * screen shows a period's: each claim's figure now (with its late charge) added up, a claim without one adding nothing.
     */
    public BigDecimal dueBetween(String sheetId, LocalDate from, LocalDate to) {
        return dueBetweenByPerson(sheetId, from, to).values().stream().reduce(ZERO, BigDecimal::add);
    }

    /**
     * Each person's due-total for the dates from..to (inclusive), by person id, every person on the sheet included
     * (0.00 when none of their counting claims is due by a date there: a claim belongs to the period of its due-by): their counting claims' figures (with late charge) added up.
     * A claim counts by the status its log records at the end of {@code to} (one added after it does not count); its
     * figure is the one it shows now, and a deferred or held claim is left out as the live totals leave it out.
     */
    public Map<Long, BigDecimal> dueBetweenByPerson(String sheetId, LocalDate from, LocalDate to) {
        List<Rate> sheetRates = rates.chain(sheetId);
        LocalDate today = sheets.today(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        Map<Long, Person> byId = new LinkedHashMap<>();
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Person p : people.list(sheetId)) {
            byId.put(p.getId(), p);
            totals.put(p.getId(), ZERO);
        }
        Map<Long, ClaimStatus> asOf = log.statusesAsOf(sheetId, to);
        for (Claim c : claims.list(sheetId)) {
            Person p = byId.get(c.getPersonId());
            ClaimStatus then = asOf.get(c.getId());
            if (p == null || then == null || !countsAs(c, then, p, today) || c.getDueBy().isBefore(from)
                    || c.getDueBy().isAfter(to)) {
                continue;
            }
            BigDecimal figure = late.due(c, figure(c, p.gradeOn(c.getDate()), sheetRates, late));
            if (figure != null) {
                totals.merge(p.getId(), figure, BigDecimal::add);
            }
        }
        return totals;
    }

    /** The claim's due as the screen shows it now: its figure with its late charge on top; null (n/a) without one. */
    public BigDecimal dueOf(Claim claim) {
        String sheetId = claim.getSheetId();
        Person p = people.find(sheetId, claim.getPersonId());
        if (p == null) {
            return null;
        }
        LateCharge late = settings.lateCharge(sheetId, sheets.today(sheetId));
        return late.due(claim, figure(claim, p.gradeOn(claim.getDate()), rates.chain(sheetId), late));
    }

    /** Record the figures the change moved, from {@code before} to the sheet's figures now. */
    public void record(String sheetId, RollupSnapshot before, ChangedBy by) {
        record(sheetId, before, by, false);
    }

    /** Record the figures the change moved; with {@code totals}, the TOTALS line even when the totals did not move. */
    public void record(String sheetId, RollupSnapshot before, ChangedBy by, boolean totals) {
        RollupSnapshot after = snapshot(sheetId);
        List<String> lines = new ArrayList<>();
        String who = " sheet=" + sheetId + " by=" + by.name();
        for (Map.Entry<Long, Figures> e : after.people().entrySet()) {
            Figures now = e.getValue();
            if (now.movedFrom(before.people().get(e.getKey()))) {
                lines.add("ROLLUP" + who + " person=" + now.name() + " claimed=" + now.claimed().toPlainString()
                        + " due-total=" + show(now.dueTotal()) + " overdue-total=" + now.overdueTotal().toPlainString());
            }
        }
        if (totals || !after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())
                || !after.overdueTotal().equals(before.overdueTotal())) {
            lines.add("TOTALS" + who + " claimed-total=" + after.claimedTotal().toPlainString() + " due-total="
                    + after.dueTotal().toPlainString() + " overdue-total=" + after.overdueTotal().toPlainString());
        }
        lines.forEach(audit::record);
    }

    /** Whether the claim counts in the live totals: its person's OPEN and PENDING claims dated within a hold do not,
     *  unless a statement binds them, nor does a claim deferred past the sheet's today; their other ones do. */
    private static boolean counts(Claim claim, Person person, LocalDate today) {
        return claim.getStatus().counts() && !held(claim, person, today) && !claim.isDeferredOn(today);
    }

    /** Whether the claim counts as it would in the live totals were its status {@code then}. */
    private static boolean countsAs(Claim claim, ClaimStatus then, Person person, LocalDate today) {
        // Held as the live totals hold it: by its status now, not the one it had then.
        boolean held = claim.getStatus().isOpen() && !claim.isStated()
                && (person.heldOn(claim.getDate()) || person.retiredOn(today));
        return then.counts() && !held && !claim.isDeferredOn(today);
    }

    /** Whether a hold leaves the claim out: it is OPEN or PENDING, unbound, and dated within one of its person's holds
     *  or its person's retirement date is reached on the sheet's today. */
    public static boolean held(Claim claim, Person person, LocalDate today) {
        return claim.getStatus().isOpen() && !claim.isBound()
                && (person.heldOn(claim.getDate()) || person.retiredOn(today));
    }

    private static String show(BigDecimal figure) {
        return figure == null ? "n/a" : figure.toPlainString();
    }

    /** A claim's figure: its settled due, else the figure a statement bound, else its rate figure (the floor comes later, after the late charge). */
    private static BigDecimal figure(Claim claim, String grade, List<Rate> sheetRates, LateCharge late) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue().setScale(2);
        }
        if (claim.isBound()) {
            return claim.getStatedDue() == null ? null : claim.getStatedDue().setScale(2);
        }
        BigDecimal rate = rateOn(sheetRates, grade, claim.getDate());
        return claim.figureAt(rate, late.rounding(claim));
    }

    /** The grade's rate in force on the date, on the sheet or else up its parents (see {@link Rate#on}). */
    private static BigDecimal rateOn(List<Rate> sheetRates, String grade, LocalDate on) {
        return Rate.on(sheetRates, grade, on);
    }
}

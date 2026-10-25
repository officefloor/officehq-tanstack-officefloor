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
import net.officefloor.hq.app.claim.ClaimRepository;
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
 * bound it to, else its amount times the rate in force on its date of the grade in force on its date, rounded half up when the claim rounds by a date before the sheet's policy date, else half to even, and at least the sheet's floor in force on its date;
 * none without a rate) with its late charge added on top while it is overdue, n/a when their grade on the sheet's today has no rate then and no counting claim keeps a settled or bound
 * figure or one at another grade, and at most the sheet's cap in force on its today (an n/a one is not capped); their overdue-total is the figures (late charge included) of
 * those of their claims that are OPEN or PENDING and past their due-by on the sheet's today, added up (uncapped). After a change, records one ROLLUP line per person whose figures (overdue-total included) it moved,
 * then a TOTALS line when any of the sheet's three totals moved. A claim deferred past the sheet's today counts in none of these. A person's OPEN and PENDING claims dated within one of their holds count in none of these, unless a statement
 * binds them; their other claims still do.
 */
@Service
public class Rollup {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;
    private final Audit audit;

    public Rollup(PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, Audit audit) {
        this.settings = settings;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.audit = audit;
    }

    public RollupSnapshot snapshot(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Rate> sheetRates = rates.list(sheetId);
        List<Claim> sheetClaims = claims.list(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        BigDecimal cap = late.settings().cap(today);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        BigDecimal dueTotal = ZERO;
        BigDecimal overdueTotal = ZERO;
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
                    if (overdue(c, today)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            BigDecimal personDue = SettingRepository.capped(
                    kept || rateOn(sheetRates, p.getGrade(), today) != null ? due : null, cap);
            figures.put(p.getId(), new Figures(p.getName(), claimed, personDue, overdue));
            claimedTotal = claimedTotal.add(claimed);
            overdueTotal = overdueTotal.add(overdue);
            if (personDue != null) {
                dueTotal = dueTotal.add(personDue);
            }
        }
        return new RollupSnapshot(figures, claimedTotal, dueTotal, overdueTotal);
    }

    /**
     * The due-total of the counting claims dated from..to (inclusive), as the screen shows a period's: each claim's
     * figure (with its late charge) added up, a claim without one adding nothing.
     */
    public BigDecimal dueBetween(String sheetId, LocalDate from, LocalDate to) {
        return dueBetweenByPerson(sheetId, from, to).values().stream().reduce(ZERO, BigDecimal::add);
    }

    /**
     * Each person's due-total for the dates from..to (inclusive), by person id, every person on the sheet included
     * (0.00 when none of their counting claims falls there): their counting claims' figures (with late charge) added up.
     */
    public Map<Long, BigDecimal> dueBetweenByPerson(String sheetId, LocalDate from, LocalDate to) {
        List<Rate> sheetRates = rates.list(sheetId);
        LocalDate today = sheets.today(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        Map<Long, Person> byId = new LinkedHashMap<>();
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Person p : people.list(sheetId)) {
            byId.put(p.getId(), p);
            totals.put(p.getId(), ZERO);
        }
        for (Claim c : claims.list(sheetId)) {
            Person p = byId.get(c.getPersonId());
            if (p == null || !counts(c, p, today) || c.getDate().isBefore(from) || c.getDate().isAfter(to)) {
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
        return late.due(claim, figure(claim, p.gradeOn(claim.getDate()), rates.list(sheetId), late));
    }

    /** Record the figures the change moved, from {@code before} to the sheet's figures now. */
    public void record(String sheetId, RollupSnapshot before, ChangedBy by) {
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
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())
                || !after.overdueTotal().equals(before.overdueTotal())) {
            lines.add("TOTALS" + who + " claimed-total=" + after.claimedTotal().toPlainString() + " due-total="
                    + after.dueTotal().toPlainString() + " overdue-total=" + after.overdueTotal().toPlainString());
        }
        lines.forEach(audit::record);
    }

    /** Whether the claim counts in the live totals: its person's OPEN and PENDING claims dated within a hold do not,
     *  unless a statement binds them, nor does a claim deferred past the sheet's today; their other ones do. */
    private static boolean counts(Claim claim, Person person, LocalDate today) {
        return claim.getStatus().counts() && !held(claim, person) && !claim.isDeferredOn(today);
    }

    /** Whether a hold leaves the claim out: it is OPEN or PENDING, unbound, and dated within one of its person's holds. */
    public static boolean held(Claim claim, Person person) {
        return claim.getStatus().isOpen() && !claim.isBound() && person.heldOn(claim.getDate());
    }

    /** Whether the claim is overdue as of the sheet's today: OPEN or PENDING, and its due-by before today. */
    private static boolean overdue(Claim claim, LocalDate today) {
        return claim.getStatus().isOpen() && claim.getDueBy() != null && claim.getDueBy().isBefore(today);
    }

    private static String show(BigDecimal figure) {
        return figure == null ? "n/a" : figure.toPlainString();
    }

    /** A claim's figure; a live one (neither settled nor bound) is at least the sheet's floor in force on its date, unless it is a correction. */
    private static BigDecimal figure(Claim claim, String grade, List<Rate> sheetRates, LateCharge late) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue().setScale(2);
        }
        if (claim.isBound()) {
            return claim.getStatedDue() == null ? null : claim.getStatedDue().setScale(2);
        }
        BigDecimal rate = rateOn(sheetRates, grade, claim.getDate());
        return claim.figureAt(rate, late.settings().floor(claim.getDate()), late.rounding(claim));
    }

    /**
     * The grade's rate in force on the date: the latest from-date on or before it, none counting as the earliest; none
     * when the date is after that rate's to-date.
     */
    private static BigDecimal rateOn(List<Rate> sheetRates, String grade, LocalDate on) {
        Rate found = null;
        for (Rate r : sheetRates) {
            if (!r.grade().equals(grade) || (r.from() != null && r.from().isAfter(on))) {
                continue;
            }
            if (found == null || (r.from() != null && (found.from() == null || r.from().isAfter(found.from())))) {
                found = r;
            }
        }
        return found == null || (found.to() != null && found.to().isBefore(on)) ? null : found.rate();
    }
}

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
 * bound it to, else its amount times the rate in force on its date of the grade in force on its date, rounded half up when the claim rounds by a date before the sheet's policy date, else half to even, and at least the sheet's floor;
 * none without a rate) with its late charge added on top while it is overdue, n/a when their grade on the sheet's today has no rate then and no counting claim keeps a settled or bound
 * figure or one at another grade. After a change, records one ROLLUP line per person whose figures it moved, then a TOTALS line when the
 * sheet's totals moved. A person's OPEN and PENDING claims dated within one of their holds count in none of these, unless a statement
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
        BigDecimal floor = settings.floor(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        BigDecimal dueTotal = ZERO;
        for (Person p : people.list(sheetId)) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            boolean kept = false;
            for (Claim c : sheetClaims) {
                if (c.getPersonId() != p.getId() || !counts(c, p)) {
                    continue;
                }
                claimed = claimed.add(c.getAmount().setScale(2));
                kept |= c.getSettledDue() != null || (c.isBound() && c.getStatedDue() != null);
                String grade = p.gradeOn(c.getDate());
                BigDecimal figure = late.due(c, figure(c, grade, sheetRates, floor, late));
                // A figure at a grade other than today's does not hang on today's grade having a rate; nor does one
                // dated on or before today, whose rate may have ended since.
                kept |= figure != null && (!grade.equals(p.getGrade()) || !c.getDate().isAfter(today));
                if (figure != null) {
                    due = due.add(figure);
                }
            }
            BigDecimal personDue = kept || rateOn(sheetRates, p.getGrade(), today) != null ? due : null;
            figures.put(p.getId(), new Figures(p.getName(), claimed, personDue));
            claimedTotal = claimedTotal.add(claimed);
            if (personDue != null) {
                dueTotal = dueTotal.add(personDue);
            }
        }
        return new RollupSnapshot(figures, claimedTotal, dueTotal);
    }

    /**
     * The due-total of the counting claims dated from..to (inclusive), as the screen shows a period's: each claim's
     * figure (with its late charge) added up, a claim without one adding nothing.
     */
    public BigDecimal dueBetween(String sheetId, LocalDate from, LocalDate to) {
        List<Rate> sheetRates = rates.list(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        LateCharge late = settings.lateCharge(sheetId, sheets.today(sheetId));
        Map<Long, Person> byId = new LinkedHashMap<>();
        for (Person p : people.list(sheetId)) {
            byId.put(p.getId(), p);
        }
        BigDecimal total = ZERO;
        for (Claim c : claims.list(sheetId)) {
            Person p = byId.get(c.getPersonId());
            if (p == null || !counts(c, p) || c.getDate().isBefore(from) || c.getDate().isAfter(to)) {
                continue;
            }
            BigDecimal figure = late.due(c, figure(c, p.gradeOn(c.getDate()), sheetRates, floor, late));
            if (figure != null) {
                total = total.add(figure);
            }
        }
        return total;
    }

    /** The claim's due as the screen shows it now: its figure with its late charge on top; null (n/a) without one. */
    public BigDecimal dueOf(Claim claim) {
        String sheetId = claim.getSheetId();
        Person p = people.find(sheetId, claim.getPersonId());
        if (p == null) {
            return null;
        }
        LateCharge late = settings.lateCharge(sheetId, sheets.today(sheetId));
        return late.due(claim, figure(claim, p.gradeOn(claim.getDate()), rates.list(sheetId), settings.floor(sheetId), late));
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
                        + " due-total=" + show(now.dueTotal()));
            }
        }
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())) {
            lines.add("TOTALS" + who + " claimed-total=" + after.claimedTotal().toPlainString() + " due-total="
                    + after.dueTotal().toPlainString());
        }
        lines.forEach(audit::record);
    }

    /** Whether the claim counts in the live totals: its person's OPEN and PENDING claims dated within a hold do not,
     *  unless a statement binds them; their other ones do. */
    private static boolean counts(Claim claim, Person person) {
        return claim.getStatus().counts() && !held(claim, person);
    }

    /** Whether a hold leaves the claim out: it is OPEN or PENDING, unbound, and dated within one of its person's holds. */
    public static boolean held(Claim claim, Person person) {
        return claim.getStatus().isOpen() && !claim.isBound() && person.heldOn(claim.getDate());
    }

    private static String show(BigDecimal figure) {
        return figure == null ? "n/a" : figure.toPlainString();
    }

    /** A claim's figure; a live one (neither settled nor bound) is at least the sheet's floor, unless it is a correction. */
    private static BigDecimal figure(Claim claim, String grade, List<Rate> sheetRates, BigDecimal floor, LateCharge late) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue().setScale(2);
        }
        if (claim.isBound()) {
            return claim.getStatedDue() == null ? null : claim.getStatedDue().setScale(2);
        }
        BigDecimal rate = rateOn(sheetRates, grade, claim.getDate());
        return claim.figureAt(rate, floor, late.rounding(claim));
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

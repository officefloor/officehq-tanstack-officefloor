package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/** Works out a sheet's figures the way the screen shows them (claimed, due-total, overdue-total, and their totals). */
@Service
public class Rollup {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final Sheets sheets;
    private final Settings settings;

    public Rollup(People people, Claims claims, Rates rates, Sheets sheets, Settings settings) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
    }

    public Figures figures(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> counting = claims.list(sheetId).stream()
                .filter(c -> c.status().counts()).toList();
        Map<Long, Figures.PersonFigures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        BigDecimal dueTotal = ZERO;
        BigDecimal overdueTotal = ZERO;
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            BigDecimal overdue = ZERO;
            boolean anyFigure = false;
            for (Claim claim : counting) {
                if (claim.personId() != person.id() || !counts(claim, person, today)) {
                    continue;
                }
                claimed = claimed.add(claim.amount());
                BigDecimal figure = figure(claim, person, today);
                if (figure != null) {
                    due = due.add(figure);
                    anyFigure = true;
                    if (overdue(claim, today)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            // With no rate today and no figure, the due-total is n/a.
            boolean none = !anyFigure && rates.on(sheetId, person.gradeOn(today), today) == null;
            // Otherwise it is held to the sheet's cap.
            due = settings.capped(sheetId, due);
            claimedTotal = claimedTotal.add(claimed);
            if (!none) {
                dueTotal = dueTotal.add(due);
            }
            overdueTotal = overdueTotal.add(overdue);
            figures.put(person.id(), new Figures.PersonFigures(person.name(), claimed.toPlainString(),
                    none ? "n/a" : due.toPlainString(), overdue.toPlainString()));
        }
        return new Figures(figures, claimedTotal.toPlainString(), dueTotal.toPlainString(),
                overdueTotal.toPlainString());
    }

    /** An open (OPEN or PENDING) claim, not deferred, whose due-by (fixed when it was made) is before {@code today}. */
    private static boolean overdue(Claim claim, LocalDate today) {
        return claim.status().isOpen() && !claim.deferredOn(today) && claim.dueBy() != null && today != null && claim.dueBy().isBefore(today);
    }

    /**
     * Each person's due-total for the dates {@code from} to {@code to} inclusive, by person id in sheet order: the
     * figures of their counting claims dated then, those without one skipped.
     */
    public Map<Long, BigDecimal> personDueTotals(String sheetId, LocalDate from, LocalDate to) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> dated = claims.list(sheetId).stream().filter(c -> c.status().counts()
                && !c.date().isBefore(from) && !c.date().isAfter(to)).toList();
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal total = ZERO;
            for (Claim claim : dated) {
                if (claim.personId() != person.id() || !counts(claim, person, today)) {
                    continue;
                }
                BigDecimal figure = figure(claim, person, today);
                if (figure != null) {
                    total = total.add(figure);
                }
            }
            totals.put(person.id(), total);
        }
        return totals;
    }

    /** The claim's figure as the screen shows it on the sheet's today (null: n/a), whatever its status. */
    public BigDecimal figure(Claim claim) {
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? claim.settledDue() : figure(claim, person, sheets.today(claim.sheetId()));
    }

    /**
     * Whether the (OPEN, PENDING or SETTLED) claim counts: a person's open claims dated within one of their holds
     * count in no live total, unless a statement bound them; nor does an OPEN claim deferred on {@code today}.
     */
    public static boolean counts(Claim claim, Person person, LocalDate today) {
        if (claim.deferredOn(today)) {
            return false;
        }
        boolean bound = claim.stated() && claim.status() != ClaimStatus.PENDING;
        return !(claim.status().isOpen() && !bound && person.heldOn(claim.date()));
    }

    /**
     * The claim's figure (null when it has none): a settled claim's frozen due, otherwise the figure its person's
     * last statement bound it to, otherwise its amount times its person's grade's rate on its date, rounded by the policy date, raised to the floor;
     * an overdue claim's late charge on {@code today} added on top of the last two.
     */
    private BigDecimal figure(Claim claim, Person person, LocalDate today) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        return settings.withLate(claim, bound(claim, person), today);
    }

    /**
     * The claim's figure before any late charge: the one its person's last statement bound, else its floored live figure.
     * A PENDING claim is not bound: it shows its live figure until approved or rejected.
     */
    private BigDecimal bound(Claim claim, Person person) {
        if (claim.stated() && claim.status() != ClaimStatus.PENDING) {
            return claim.statedDue();
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.gradeOn(claim.date()), claim.date());
        return settings.live(claim, rate);
    }
}

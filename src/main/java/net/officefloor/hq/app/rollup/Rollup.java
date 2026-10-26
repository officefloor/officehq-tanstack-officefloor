package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

/** Works out a sheet's figures the way the screen shows them (claimed, due-total, and their totals). */
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
                .filter(c -> c.status() == ClaimStatus.OPEN || c.status() == ClaimStatus.SETTLED).toList();
        Map<Long, Figures.PersonFigures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        BigDecimal dueTotal = ZERO;
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            boolean anyFigure = false;
            for (Claim claim : counting) {
                if (claim.personId() != person.id() || !counts(claim, person)) {
                    continue;
                }
                claimed = claimed.add(claim.amount());
                BigDecimal figure = figure(claim, person, today);
                if (figure != null) {
                    due = due.add(figure);
                    anyFigure = true;
                }
            }
            // With no rate today and no figure, the due-total is n/a.
            boolean none = !anyFigure && rates.on(sheetId, person.grade(), today) == null;
            claimedTotal = claimedTotal.add(claimed);
            if (!none) {
                dueTotal = dueTotal.add(due);
            }
            figures.put(person.id(), new Figures.PersonFigures(person.name(), claimed.toPlainString(),
                    none ? "n/a" : due.toPlainString()));
        }
        return new Figures(figures, claimedTotal.toPlainString(), dueTotal.toPlainString());
    }

    /** The figures of the sheet's counting claims dated from {@code from} to {@code to} inclusive, those without one skipped. */
    public BigDecimal dueTotal(String sheetId, LocalDate from, LocalDate to) {
        LocalDate today = sheets.today(sheetId);
        BigDecimal total = ZERO;
        for (Claim claim : claims.list(sheetId)) {
            if ((claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.SETTLED)
                    || claim.date().isBefore(from) || claim.date().isAfter(to)) {
                continue;
            }
            Person person = people.find(sheetId, claim.personId());
            if (person != null && !counts(claim, person)) {
                continue;
            }
            BigDecimal figure = person == null ? claim.settledDue() : figure(claim, person, today);
            if (figure != null) {
                total = total.add(figure);
            }
        }
        return total;
    }

    /** Whether the (OPEN or SETTLED) claim counts: a held person's OPEN claims count in no live total. */
    private static boolean counts(Claim claim, Person person) {
        return !(person.held() && claim.status() == ClaimStatus.OPEN);
    }

    /**
     * The claim's figure (null when it has none): a settled claim's frozen due, otherwise the figure its person's
     * last statement bound it to, otherwise its amount times its person's grade's rate on its date, half to even, raised to the floor;
     * an overdue claim's late charge on {@code today} added on top of the last two.
     */
    private BigDecimal figure(Claim claim, Person person, LocalDate today) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        return settings.withLate(claim, bound(claim, person), today);
    }

    /** The claim's figure before any late charge: the one its person's last statement bound, else its floored live figure. */
    private BigDecimal bound(Claim claim, Person person) {
        if (claim.stated()) {
            return claim.statedDue();
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.grade(), claim.date());
        return rate == null ? null : settings.floored(claim.sheetId(), claim.amount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN));
    }
}

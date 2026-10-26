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

    public Rollup(People people, Claims claims, Rates rates, Sheets sheets) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
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
                if (claim.personId() != person.id()) {
                    continue;
                }
                claimed = claimed.add(claim.amount());
                BigDecimal figure = figure(claim, person);
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
        BigDecimal total = ZERO;
        for (Claim claim : claims.list(sheetId)) {
            if ((claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.SETTLED)
                    || claim.date().isBefore(from) || claim.date().isAfter(to)) {
                continue;
            }
            Person person = people.find(sheetId, claim.personId());
            BigDecimal figure = person == null ? claim.settledDue() : figure(claim, person);
            if (figure != null) {
                total = total.add(figure);
            }
        }
        return total;
    }

    /**
     * The claim's figure (null when it has none): a settled claim's frozen due, otherwise the figure its person's
     * last statement bound it to, otherwise its amount times its person's grade's rate on its date, half to even.
     */
    private BigDecimal figure(Claim claim, Person person) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.stated()) {
            return claim.statedDue();
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.grade(), claim.date());
        return rate == null ? null : claim.amount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}

package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Floor;
import net.officefloor.hq.app.setting.LatePct;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them: each person's claimed and due-total, in the order
 * they were added, and the sheet's claimed-total and due-total.
 *
 * Only OPEN and SETTLED claims count, and a held person's OPEN claims do not. A claim's figure is its settled due, else the figure a statement
 * bound it to, else its live due at the person's grade's rate in force on its date, at least the sheet's floor
 * (n/a without a rate), plus the late charge of an overdue OPEN claim (see {@link LatePct}).
 * A person's due-total adds up their counting claims' figures, skipping n/a; it is n/a when the grade
 * in force on the sheet's today has no rate there and no counting claim has a figure. The sheet's due-total
 * adds up the people's due-totals, skipping n/a.
 */
@Service
public class Rollup {

    static final String NA = "n/a";

    /** One person's figures, as text with two decimals (due-total may be n/a). */
    public record Figures(String name, String claimed, String dueTotal) {
        boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal);
        }
    }

    /** The sheet's figures at one moment: people by id, in the order they were added, and the totals. */
    public record Snapshot(Map<Long, Figures> people, String claimedTotal, String dueTotal) {
        boolean sameTotals(Snapshot other) {
            return claimedTotal.equals(other.claimedTotal) && dueTotal.equals(other.dueTotal);
        }
    }

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetClock clock;
    private final Floor floor;
    private final LatePct latePct;

    public Rollup(PersonRepository people, ClaimRepository claims, RateRepository rates, SheetClock clock, Floor floor,
            LatePct latePct) {
        this.floor = floor;
        this.latePct = latePct;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.clock = clock;
    }

    public Snapshot take(String sheetId) {
        LocalDate today = clock.today(sheetId);
        List<Rate> sheetRates = rates.findBySheet(sheetId);
        List<Claim> sheetClaims = claims.findBySheet(sheetId);
        BigDecimal least = floor.of(sheetId);
        LatePct.Charge late = latePct.of(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = zero();
        BigDecimal dueTotal = zero();
        for (Person person : people.findBySheet(sheetId)) {
            BigDecimal claimed = zero();
            BigDecimal due = zero();
            boolean figured = false;
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId()) continue;
                if (!person.counts(claim.getStatus())) continue;
                claimed = claimed.add(claim.getAmount());
                BigDecimal figure = late.addTo(claim, figure(claim, person, sheetRates, least));
                if (figure != null) {
                    due = due.add(figure);
                    figured = true;
                }
            }
            boolean na = Rate.inForce(sheetRates, person.gradeOn(today), today).isEmpty() && !figured;
            claimedTotal = claimedTotal.add(claimed);
            if (!na) dueTotal = dueTotal.add(due);
            figures.put(person.getId(), new Figures(person.getName(), text(claimed), na ? NA : text(due)));
        }
        return new Snapshot(figures, text(claimedTotal), text(dueTotal));
    }

    /** The due-total of the sheet's counting claims dated from {@code from} to {@code to}, both included:
     *  each claim's figure at its person's grade, skipping n/a. */
    public BigDecimal dueTotal(String sheetId, LocalDate from, LocalDate to) {
        List<Rate> sheetRates = rates.findBySheet(sheetId);
        List<Claim> sheetClaims = claims.findBySheet(sheetId);
        BigDecimal least = floor.of(sheetId);
        LatePct.Charge late = latePct.of(sheetId);
        BigDecimal sum = zero();
        for (Person person : people.findBySheet(sheetId)) {
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId()) continue;
                if (!person.counts(claim.getStatus())) continue;
                if (claim.getDate().isBefore(from) || claim.getDate().isAfter(to)) continue;
                BigDecimal figure = late.addTo(claim, figure(claim, person, sheetRates, least));
                if (figure != null) sum = sum.add(figure);
            }
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The ROLLUP lines for each person whose figures moved since {@code before} (a new person counts
     *  as moved), then a TOTALS line when the sheet's totals moved. */
    public List<String> changes(String sheetId, String user, Snapshot before, Snapshot after) {
        String prefix = " sheet=" + sheetId + " by=" + user;
        List<String> lines = new ArrayList<>();
        after.people().forEach((id, now) -> {
            if (!now.sameAs(before.people().get(id))) {
                lines.add("ROLLUP" + prefix + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + now.dueTotal());
            }
        });
        if (!after.sameTotals(before)) {
            lines.add("TOTALS" + prefix + " claimed-total=" + after.claimedTotal() + " due-total=" + after.dueTotal());
        }
        return lines;
    }

    private static BigDecimal figure(Claim claim, Person person, List<Rate> rates, BigDecimal least) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        if (claim.isStated()) return claim.getStatedDue();
        return Floor.raise(Rate.inForce(rates, person.gradeOn(claim.getDate()), claim.getDate())
                .map(r -> claim.getAmount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .orElse(null), least);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static String text(BigDecimal value) {
        return Objects.requireNonNull(value).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }
}

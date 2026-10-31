package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/**
 * The figures the screen shows — each person's claimed and due-total, the sheet's claimed-total and
 * due-total — worked out as the screen does, so a change can audit the ones it moved: one ROLLUP
 * line per person whose figures changed (a new person counts as changed), then a TOTALS line when
 * the sheet's totals changed.
 */
@Service
public class Rollup {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final People people;
    private final Claims claims;
    private final Figures figures;
    private final Rates rates;
    private final Sheets sheets;
    private final Audit audit;

    public Rollup(People people, Claims claims, Figures figures, Rates rates, Sheets sheets, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.figures = figures;
        this.rates = rates;
        this.sheets = sheets;
        this.audit = audit;
    }

    /** The sheet's figures now. A person's due-total is the sum of their counting claims' figures (a held
     *  person's OPEN claims do not count),
     *  n/a when there is no rate today and none has a figure; the sheet's skips the n/a ones. */
    public Snapshot snapshot(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.onSheet(sheetId);
        Map<Long, Snapshot.PersonFigures> figuresOf = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        BigDecimal dueTotal = ZERO;
        for (Person person : people.onSheet(sheetId)) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            boolean any = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !person.counts(claim.getStatus())) continue;
                claimed = claimed.add(claim.getAmount());
                BigDecimal figure = figures.figure(claim, person.getGrade());
                if (figure != null) {
                    due = due.add(figure);
                    any = true;
                }
            }
            if (!any && rates.pct(sheetId, person.getGrade(), today) == null) due = null;
            figuresOf.put(person.getId(), new Snapshot.PersonFigures(person.getName(), claimed, due));
            claimedTotal = claimedTotal.add(claimed);
            if (due != null) dueTotal = dueTotal.add(due);
        }
        return new Snapshot(figuresOf, claimedTotal, dueTotal);
    }

    /** Audit the figures that moved between `before` and the sheet now. */
    public void record(String sheetId, Snapshot before, String user) {
        Snapshot after = snapshot(sheetId);
        String prefix = " sheet=" + sheetId + " by=" + user;
        after.people().forEach((id, now) -> {
            Snapshot.PersonFigures was = before.people().get(id);
            if (was != null && same(was.claimed(), now.claimed()) && same(was.dueTotal(), now.dueTotal())) return;
            audit.record("ROLLUP" + prefix + " person=" + now.name() + " claimed=" + show(now.claimed())
                    + " due-total=" + show(now.dueTotal()));
        });
        if (!same(before.claimedTotal(), after.claimedTotal()) || !same(before.dueTotal(), after.dueTotal())) {
            audit.record("TOTALS" + prefix + " claimed-total=" + show(after.claimedTotal())
                    + " due-total=" + show(after.dueTotal()));
        }
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? Objects.equals(a, b) : a.compareTo(b) == 0;
    }

    private static String show(BigDecimal value) {
        return value == null ? "n/a" : value.setScale(2).toPlainString();
    }
}

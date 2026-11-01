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
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them: each person's claimed and due-total, and the sheet's claimed-total
 * and due-total. A change takes them before it writes, then {@link #record} audits the ones it moved: a ROLLUP line
 * per person whose figures changed (in the order they were added; a new person counts as changed), then a TOTALS
 * line when the sheet's totals changed.
 */
@Service
public class Rollup {

    /** A person's figures: claimed (two decimals) and due-total (two decimals, or n/a). */
    public record Figures(String name, String claimed, String dueTotal) {
    }

    /** The sheet's figures at one moment: per person id, in the order they were added, and the totals. */
    public record Snapshot(Map<Long, Figures> people, String claimedTotal, String dueTotal) {
    }

    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final SheetClock clock;
    private final Settings settings;
    private final Audit audit;

    public Rollup(People people, Claims claims, Rates rates, SheetClock clock, Settings settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.clock = clock;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures now. */
    public Snapshot take(String sheet) {
        LocalDate today = clock.today(sheet);
        List<Rate> sheetRates = rates.list(sheet);
        BigDecimal floor = settings.floor(sheet);
        Late late = settings.late(sheet, today);
        List<Claim> all = claims.list(sheet);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.list(sheet)) {
            List<Claim> mine = all.stream().filter((c) -> c.person() == person.id() && person.counts(c)).toList();
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            for (Claim claim : mine) {
                claimed = claimed.add(new BigDecimal(claim.amount()));
                BigDecimal d = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
                due = d == null ? due : due.add(d);
            }
            // n/a when the grade has no rate on the sheet's today, unless a claim has a figure apart from it.
            boolean na = Rate.on(sheetRates, person.grade(), today) == null
                    && mine.stream().noneMatch((c) -> person.figureApartFrom(c, today, sheetRates, floor));
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dueTotal = dueTotal.add(due);
            }
            figures.put(person.id(), new Figures(person.name(), claimed.toPlainString(), na ? "n/a" : due.toPlainString()));
        }
        return new Snapshot(figures, claimedTotal.toPlainString(), dueTotal.toPlainString());
    }

    /** Audits the figures the change moved since {@code before}, after the change's own lines. */
    public void record(String sheet, String user, Snapshot before) {
        Snapshot after = take(sheet);
        String prefix = " sheet=" + sheet + " by=" + user + " ";
        for (Map.Entry<Long, Figures> entry : after.people().entrySet()) {
            Figures now = entry.getValue();
            Figures was = before.people().get(entry.getKey());
            if (was == null || !was.claimed().equals(now.claimed()) || !was.dueTotal().equals(now.dueTotal())) {
                audit.record("ROLLUP" + prefix + "person=" + now.name() + " claimed=" + now.claimed() + " due-total="
                        + now.dueTotal());
            }
        }
        if (!Objects.equals(before.claimedTotal(), after.claimedTotal())
                || !Objects.equals(before.dueTotal(), after.dueTotal())) {
            audit.record("TOTALS" + prefix + "claimed-total=" + after.claimedTotal() + " due-total=" + after.dueTotal());
        }
    }
}

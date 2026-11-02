package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/** The sheet's figures as shown: each person's claimed and due-total, and the sheet's claimed-total and
 *  due-total. A change takes them before it writes, then after its own audit lines records one ROLLUP
 *  line per person whose figures moved (people in the order they were added; a new person counts as
 *  moved), then a TOTALS line when the sheet's totals moved. */
@Service
public class Rollup {

    /** A person's figures: claimed (two decimals) and due-total (two decimals, or 'n/a'). */
    record Figures(String name, String claimed, String dueTotal) {
        boolean same(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal);
        }
    }

    /** The figures of a sheet at one moment, people by id in the order they were added. */
    public record Snapshot(Map<Long, Figures> people, String claimedTotal, String dueTotal) {
    }

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final SheetRepository sheets;
    private final Audit audit;

    public Rollup(PersonRepository people, ClaimRepository claims, SheetRepository sheets, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.sheets = sheets;
        this.audit = audit;
    }

    /** The sheet's figures now. A person's due-total is the sum of their counting claims' figures,
     *  skipping those with none; 'n/a' when no rate for their grade is in force on the sheet's today and
     *  no counting claim keeps a fixed figure. The sheet's due-total skips 'n/a'. */
    public Snapshot take(String sheetId) {
        List<Person> everyone = people.list(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        if (!everyone.isEmpty()) {
            LocalDate today = sheets.today(sheetId);
            List<Claim> all = claims.list(sheetId);
            for (Person p : everyone) {
                BigDecimal claimed = BigDecimal.ZERO.setScale(2);
                BigDecimal due = BigDecimal.ZERO.setScale(2);
                boolean fixed = false;
                for (Claim c : all) {
                    if (c.personId() != p.id() || !c.counts()) continue;
                    claimed = claimed.add(new BigDecimal(c.amount()));
                    fixed |= c.due() != null || (c.stated() && c.statedDue() != null);
                    BigDecimal figure = claims.figure(sheetId, c);
                    if (figure != null) due = due.add(figure);
                }
                boolean shown = fixed || claims.rated(sheetId, p.id(), today);
                figures.put(p.id(), new Figures(p.name(), claimed.toPlainString(),
                        shown ? due.toPlainString() : "n/a"));
                claimedTotal = claimedTotal.add(claimed);
                if (shown) dueTotal = dueTotal.add(due);
            }
        }
        return new Snapshot(figures, claimedTotal.toPlainString(), dueTotal.toPlainString());
    }

    /** Record the lines for whatever figures moved since {@code before}. */
    public void record(String sheetId, String by, Snapshot before) {
        Snapshot after = take(sheetId);
        for (Map.Entry<Long, Figures> e : after.people().entrySet()) {
            Figures now = e.getValue();
            if (now.same(before.people().get(e.getKey()))) continue;
            audit.record("ROLLUP sheet=" + sheetId + " by=" + by + " person=" + now.name() + " claimed="
                    + now.claimed() + " due-total=" + now.dueTotal());
        }
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())) {
            audit.record("TOTALS sheet=" + sheetId + " by=" + by + " claimed-total=" + after.claimedTotal()
                    + " due-total=" + after.dueTotal());
        }
    }
}

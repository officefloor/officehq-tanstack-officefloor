package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them — each person's claimed and due-total, and the
 * sheet's claimed-total and due-total — worked out on the server so a change can audit what it
 * moved: after the change's own lines, one ROLLUP line per person whose figures changed (people in
 * the order they were added; a new person counts as changed), then a TOTALS line when the sheet's
 * totals changed.
 */
@Service
public class Rollup {

    private static final String NA = "n/a";

    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final Sheets sheets;
    private final Audit audit;

    public Rollup(People people, Claims claims, Rates rates, Sheets sheets, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.audit = audit;
    }

    /** The sheet's figures now. */
    public RollupFigures figures(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, RollupFigures.PersonFigures> byPerson = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean anyFixed = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !claim.getStatus().counts()) {
                    continue;
                }
                claimed = claimed.add(claim.getAmount());
                BigDecimal fixed = ClaimResponse.fixedDue(claim);
                anyFixed |= fixed != null;
                BigDecimal figure = fixed != null || claim.isStated() ? fixed
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.getGrade(), claim.getDate()));
                if (figure != null) {
                    due = due.add(figure);
                }
            }
            boolean na = !anyFixed && rates.find(sheetId, person.getGrade(), today) == null;
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dueTotal = dueTotal.add(due);
            }
            byPerson.put(person.getId(), new RollupFigures.PersonFigures(person.getName(),
                    show(claimed), na ? NA : show(due)));
        }
        return new RollupFigures(byPerson, show(claimedTotal), show(dueTotal));
    }

    /** Audit (on commit) the figures that moved since {@code before}, as taken by {@code user}. */
    public void recordChanges(String sheetId, String user, RollupFigures before) {
        RollupFigures after = figures(sheetId);
        String prefix = " sheet=" + sheetId + " by=" + user;
        after.people().forEach((id, now) -> {
            RollupFigures.PersonFigures was = before.people().get(id);
            if (was == null || !Objects.equals(was.claimed(), now.claimed())
                    || !Objects.equals(was.dueTotal(), now.dueTotal())) {
                audit.recordOnCommit("ROLLUP" + prefix + " person=" + now.name()
                        + " claimed=" + now.claimed() + " due-total=" + now.dueTotal());
            }
        });
        if (!before.claimedTotal().equals(after.claimedTotal()) || !before.dueTotal().equals(after.dueTotal())) {
            audit.recordOnCommit("TOTALS" + prefix + " claimed-total=" + after.claimedTotal()
                    + " due-total=" + after.dueTotal());
        }
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

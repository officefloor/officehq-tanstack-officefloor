package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
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
    private final Settings settings;
    private final Audit audit;

    public Rollup(People people, Claims claims, Rates rates, Sheets sheets, Settings settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures now. */
    public RollupFigures figures(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        BigDecimal latePct = settings.latePct(sheetId);
        LocalDate policyDate = settings.policyDate(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, RollupFigures.PersonFigures> byPerson = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean anyFigure = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !person.countsLive(claim)) {
                    continue;
                }
                claimed = claimed.add(claim.getAmount());
                RoundingMode rounding = Rounding.of(claim, policyDate);
                BigDecimal fixed = ClaimResponse.fixedDue(claim);
                anyFigure |= fixed != null;
                BigDecimal figure = fixed != null || claim.isBound() ? fixed
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()), floor, rounding);
                if (figure != null) {
                    anyFigure = true;
                    due = due.add(LateCharge.withCharge(claim, figure, latePct, today, rounding));
                }
            }
            // n/a only when no claim keeps or gets a figure (each at the grade in force on its date).
            boolean na = !anyFigure && rates.find(sheetId, person.getGrade(), today) == null;
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

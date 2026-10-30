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
 * sheet's claimed-total and due-total, each person's due-total held to the sheet's cap — worked out on the server so a change can audit what it
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
        BigDecimal cap = settings.cap(sheetId);
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
                BigDecimal figure = due(claim, person, today, floor, latePct, policyDate);
                if (figure != null) {
                    anyFigure = true;
                    due = due.add(figure);
                }
            }
            // n/a only when no claim keeps or gets a figure (each at the grade in force on its date).
            boolean na = !anyFigure && rates.find(sheetId, person.getGrade(), today) == null;
            due = Settings.capped(due, cap);
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dueTotal = dueTotal.add(due);
            }
            byPerson.put(person.getId(), new RollupFigures.PersonFigures(person.getName(),
                    show(claimed), na ? NA : show(due)));
        }
        return new RollupFigures(byPerson, show(claimedTotal), show(dueTotal));
    }

    /** A claim's due as the screen shows it now; null for n/a. */
    public BigDecimal due(Claim claim, Person person) {
        String sheetId = claim.getSheetId();
        return due(claim, person, sheets.today(sheetId), settings.floor(sheetId), settings.latePct(sheetId),
                settings.policyDate(sheetId));
    }

    /** A claim's due: fixed when settled or bound by a statement, else its amount (raised to the floor) at the rate
     *  of its person's grade in force on its date; with its late charge as of today on top. */
    private BigDecimal due(Claim claim, Person person, LocalDate today, BigDecimal floor, BigDecimal latePct,
            LocalDate policyDate) {
        RoundingMode rounding = Rounding.of(claim, policyDate);
        BigDecimal fixed = ClaimResponse.fixedDue(claim);
        BigDecimal figure = fixed != null || claim.isBound() ? fixed
                : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate()),
                        floor, rounding);
        return LateCharge.withCharge(claim, figure, latePct, today, rounding);
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

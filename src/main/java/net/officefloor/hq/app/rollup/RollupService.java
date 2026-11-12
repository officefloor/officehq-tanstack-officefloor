package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.stereotype.Service;

/**
 * Works out the sheet's figures as the screen shows them, and records in the audit file how a change
 * moved them: ROLLUP sheet=<id> by=<user> person=<name> claimed=<0.00> due-total=<0.00 or n/a> for each
 * person whose figures changed (a person new to the sheet counts as changed), in the order they were
 * added, then TOTALS sheet=<id> by=<user> claimed-total=<0.00> due-total=<0.00> when the sheet's totals
 * changed. A change that moves no figure records nothing here.
 */
@Service
public class RollupService {

    private static final String NA = "n/a";

    private final PersonStore people;
    private final ClaimStore claims;
    private final RateStore rates;
    private final SheetStore sheets;
    private final Audit audit;

    public RollupService(PersonStore people, ClaimStore claims, RateStore rates, SheetStore sheets, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.audit = audit;
    }

    /** The sheet's figures as the store holds them now. */
    public Rollup snapshot(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean fixed = false;
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId()
                        || !counts(claim, person)) {
                    continue;
                }
                claimed = claimed.add(claim.getAmount());
                BigDecimal figure = ClaimFigure.of(claim, rates.find(sheetId, person.getGrade(), claim.getDate()));
                if (figure != null) {
                    due = due.add(figure);
                }
                fixed |= claim.getSettledDue() != null || (claim.isStated() && claim.getStatedDue() != null);
            }
            boolean na = rates.find(sheetId, person.getGrade(), today) == null && !fixed;
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dueTotal = dueTotal.add(due);
            }
            figures.put(person.getId(), new Rollup.Figures(person.getName(), shown(claimed), na ? NA : shown(due)));
        }
        return new Rollup(figures, shown(claimedTotal), shown(dueTotal));
    }

    /** The due-total of the claims that count dated from..to (inclusive): the sum of their figures,
     *  skipping n/a, as the screen shows a period's due-total. */
    public BigDecimal dueBetween(String sheetId, LocalDate from, LocalDate to) {
        List<Claim> all = claims.list(sheetId);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId()
                        || !counts(claim, person)
                        || claim.getDate().isBefore(from) || claim.getDate().isAfter(to)) {
                    continue;
                }
                BigDecimal figure = ClaimFigure.of(claim, rates.find(sheetId, person.getGrade(), claim.getDate()));
                if (figure != null) {
                    total = total.add(figure);
                }
            }
        }
        return total;
    }

    /** Records how the sheet's figures moved since {@code before}, the change made by {@code user}. */
    public void record(String sheetId, String user, Rollup before) {
        Rollup after = snapshot(sheetId);
        String who = " sheet=" + sheetId + " by=" + user;
        after.people().forEach((id, now) -> {
            if (!now.sameAs(before.people().get(id))) {
                audit.record("ROLLUP" + who + " person=" + now.name() + " claimed=" + now.claimed() + " due-total="
                        + now.dueTotal());
            }
        });
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())) {
            audit.record("TOTALS" + who + " claimed-total=" + after.claimedTotal() + " due-total=" + after.dueTotal());
        }
    }

    /** Only OPEN and SETTLED claims count, and a held person's OPEN claims do not. */
    private static boolean counts(Claim claim, Person person) {
        return claim.getStatus() == ClaimStatus.SETTLED
                || (claim.getStatus() == ClaimStatus.OPEN && !person.isHeld());
    }

    private static String shown(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

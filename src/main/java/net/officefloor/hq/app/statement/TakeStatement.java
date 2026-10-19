package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes a statement for the person: works out their due-total as the screen shows it, binds each of
 * their claims that counts (OPEN or SETTLED) to the figure it has now, unbinds the rest, and moves
 * the person's statement count, last due-total and date (the sheet's today) on.
 *
 * A claim's figure is its settled due, else the figure a statement bound it to, else its live due at
 * the person's grade's rate in force on its date (n/a without one). The due-total adds up the counting
 * claims' figures, skipping n/a; it is n/a when the grade has no rate on today and no counting claim
 * keeps a fixed figure.
 */
public class TakeStatement {
    public void take(@Val Person person, ClaimRepository claims, PersonRepository people, RateRepository rateRepository,
            SheetClock clock, Out<Statement> statementOut) {
        String sheetId = person.getSheetId();
        LocalDate today = clock.today(sheetId);
        List<Rate> rates = rateRepository.findBySheet(sheetId);
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        for (Claim claim : claims.findByPerson(sheetId, person.getId())) {
            boolean counts = claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
            if (!counts) {
                claim.setStated(false);
                claim.setStatedDue(null);
            } else {
                BigDecimal figure = figure(claim, person, rates);
                if (claim.getSettledDue() != null || (claim.isStated() && claim.getStatedDue() != null)) fixed = true;
                if (figure != null) sum = sum.add(figure);
                claim.setStated(true);
                claim.setStatedDue(figure);
                counted++;
            }
            claims.save(claim);
        }
        BigDecimal dueTotal = Rate.inForce(rates, person.getGrade(), today).isEmpty() && !fixed ? null : sum;
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(dueTotal);
        person.setStatementDate(today);
        people.saveStatement(person);
        statementOut.set(new Statement(dueTotal, counted));
    }

    /** amount × pct / 100, rounded half to even to two decimals, unless already fixed; null when n/a. */
    private static BigDecimal figure(Claim claim, Person person, List<Rate> rates) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        if (claim.isStated()) return claim.getStatedDue();
        return Rate.inForce(rates, person.getGrade(), claim.getDate())
                .map(r -> claim.getAmount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .orElse(null);
    }
}

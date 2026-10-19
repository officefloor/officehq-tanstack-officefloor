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
import net.officefloor.hq.app.setting.Floor;
import net.officefloor.hq.app.setting.LatePct;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes a statement for the person: works out their due-total as the screen shows it, binds each of
 * their claims that counts (OPEN or SETTLED) to the figure it has now, unbinds the rest
 * (leaving a held person's OPEN claims, which count in no total, as they are), and moves
 * the person's statement count, last due-total and date (the sheet's today) on.
 *
 * A claim's figure is its settled due, else the figure a statement bound it to, else its live due at
 * the rate of the person's grade in force on its date (the grade in force on that date too), at least the sheet's floor (n/a without a rate). The due-total adds up the counting
 * claims' figures, each with its late charge on top (see {@link LatePct}; the bound figure leaves it out, so it
 * keeps following the sheet's today), skipping n/a; it is n/a when the grade in force on today has no rate there and no counting claim
 * has a figure.
 */
public class TakeStatement {
    public void take(@Val Person person, ClaimRepository claims, PersonRepository people, RateRepository rateRepository,
            SheetClock clock, Floor floor, LatePct latePct, Out<Statement> statementOut) {
        String sheetId = person.getSheetId();
        LocalDate today = clock.today(sheetId);
        List<Rate> rates = rateRepository.findBySheet(sheetId);
        BigDecimal least = floor.of(sheetId);
        LatePct.Charge late = latePct.of(sheetId);
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        boolean figured = false;
        int counted = 0;
        for (Claim claim : claims.findByPerson(sheetId, person.getId())) {
            boolean counts = claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
            if (counts && !person.counts(claim.getStatus())) {
                continue;           // a held person's OPEN claim: in no total, and left as it is
            } else if (!counts) {
                claim.setStated(false);
                claim.setStatedDue(null);
            } else {
                BigDecimal figure = figure(claim, person, rates, least);
                if (figure != null) figured = true;
                BigDecimal due = late.addTo(claim, figure);
                if (due != null) sum = sum.add(due);
                claim.setStated(true);
                claim.setStatedDue(figure);
                counted++;
            }
            claims.save(claim);
        }
        BigDecimal dueTotal = Rate.inForce(rates, person.gradeOn(today), today).isEmpty() && !figured ? null : sum;
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(dueTotal);
        person.setStatementDate(today);
        people.saveStatement(person);
        statementOut.set(new Statement(dueTotal, counted));
    }

    /** amount × pct / 100, rounded half to even to two decimals and at least the floor, unless already fixed; null when n/a. */
    private static BigDecimal figure(Claim claim, Person person, List<Rate> rates, BigDecimal least) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        if (claim.isStated()) return claim.getStatedDue();
        return Floor.raise(claim, Rate.inForce(rates, person.gradeOn(claim.getDate()), claim.getDate())
                .map(r -> claim.getAmount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .orElse(null), least);
    }
}

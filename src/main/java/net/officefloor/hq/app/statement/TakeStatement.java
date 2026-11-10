package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement: their due-total now (the figures of their claims that count, added
 * up, skipping n/a; n/a when their grade has no rate today and no claim that counts keeps a fixed
 * figure), as the screen shows it. Every claim of theirs that counts is bound to its figure now;
 * every other claim of theirs is left unbound (live). The person's statement count goes up by one,
 * and the due-total and the sheet's today are kept as the last statement's.
 */
public class TakeStatement {
    public void take(@Val Sheet sheet, @Val Person person, ClaimRepository claims, RateRepository rates,
            PersonRepository people, Out<Statement> statementOut) {
        List<Rate> all = rates.findAll(sheet.id());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
            boolean counts = claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
            if (counts) {
                counted++;
                if (claim.getSettledDue() != null || claim.isStated()) fixed = true;
                BigDecimal figure = ClaimFigure.of(claim, all, person.getGrade());
                if (figure != null) total = total.add(figure);
                claim.bind(true, figure);
            } else {
                claim.bind(false, null);
            }
            claims.save(claim);
        }
        String dueTotal = Rate.inForce(all, person.getGrade(), sheet.today()).isEmpty() && !fixed
                ? "n/a" : total.toPlainString();
        person.setStatements(person.getStatementCount() + 1, dueTotal, sheet.today());
        people.saveStatements(person);
        statementOut.set(new Statement(dueTotal, counted));
    }
}

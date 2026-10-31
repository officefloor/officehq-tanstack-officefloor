package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes the person's statement: their due-total now, the sum of their counting claims' figures
 *  with any late charges (n/a when there is no rate today and none has a figure), dated the sheet's today. Every claim
 *  of theirs that counts (not an OPEN one while they are held) is bound to the figure it has now, without its late charge; every other one is unbound. */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, Claims claims, Figures figures,
            Rates rates, Sheets sheets, People people, Out<Statement> statement) {
        Person locked = people.lock(person.getSheetId(), person.getId());
        person.setStatementCount(locked.getStatementCount());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean any = false;
        int counted = 0;
        for (Claim claim : claims.ofPerson(person.getSheetId(), person.getId())) {
            if (person.counts(claim.getStatus())) {
                BigDecimal bound = figures.figure(claim, person.gradeOn(claim.getDate()));
                // The statement binds the figure; the late charge on top keeps following today.
                BigDecimal figure = bound == null || claim.getDue() != null ? bound : bound.add(figures.charge(claim, bound));
                claim.setStated(true);
                claim.setStatedDue(bound);
                counted++;
                if (figure != null) {
                    total = total.add(figure);
                    any = true;
                }
            } else {
                claim.setStated(false);
                claim.setStatedDue(null);
            }
            claims.save(claim);
        }
        var today = sheets.today(person.getSheetId());
        BigDecimal dueTotal = !any && rates.pct(person.getSheetId(), person.gradeOn(today), today) == null ? null : total;
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(dueTotal);
        person.setStatementDate(today);
        people.saveStatement(person);
        statement.set(new Statement(dueTotal, counted, request.user()));
    }
}

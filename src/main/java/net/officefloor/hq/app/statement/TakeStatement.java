package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement: records their due-total now, binds each of their claims that counts to the
 * figure it has now, and unbinds (live again) each that does not. The due-total is their counting claims'
 * figures added up; n/a when their grade has no rate on the sheet's today and no counting claim keeps a figure.
 */
public class TakeStatement {
    public void take(@Val Person person, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SheetRepository sheets, Out<Statement> statement, Out<Person> updated) {
        LocalDate today = sheets.today(person.sheetId());
        List<Rate> sheetRates = rates.findAll(person.sheetId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int counted = 0;
        for (Claim claim : claims.findForPerson(person.sheetId(), person.id())) {
            if (!person.counts(claim)) {
                claims.bindStatement(claim, false, null);
                continue;
            }
            boolean fixed = claim.due() != null || (claim.stated() && claim.statedDue() != null);
            BigDecimal figure = claim.figure(Rate.inForce(sheetRates, person.grade(), claim.date()));
            claims.bindStatement(claim, true, figure);
            counted++;
            kept |= fixed;
            if (figure != null) {
                total = total.add(figure);
            }
        }
        String dueTotal = Rate.inForce(sheetRates, person.grade(), today) == null && !kept ? "n/a"
                : total.setScale(2).toPlainString();
        statement.set(new Statement(person.name(), dueTotal, counted));
        updated.set(people.recordStatement(person, dueTotal, today));
    }
}

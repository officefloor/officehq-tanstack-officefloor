package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Take the person's statement: record their due-total now and the sheet's today, bind each of their claims
 *  that counts to the figure it shows now, and unbind the rest (their due is live again). A settled claim's
 *  fixed due is left as it is. */
public class SaveStatement {
    public void save(@Val Person loaded, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, Out<Statement> statement, Out<PersonResponse> saved) {
        people.lock(loaded);
        Person person = people.find(loaded.sheetId(), loaded.id());
        var today = sheets.today(person.sheetId());
        List<Claim> theirs = claims.listForPerson(person.sheetId(), person.id());
        BigDecimal total = Dues.total(person.sheetId(), theirs, person.grade(), person.held(), today, rates);
        int counted = 0;
        for (Claim c : theirs) {
            if (Dues.counts(c, person.held())) {
                counted++;
                claims.state(c, true, Dues.figure(c, person.grade(), rates));
            } else if (c.stated()) {
                claims.state(c, false, null);
            }
        }
        people.stated(person, total, today);
        statement.set(new Statement(person.sheetId(), person.name(), total, counted));
        saved.set(PersonResponse.from(people.find(person.sheetId(), person.id())));
    }
}

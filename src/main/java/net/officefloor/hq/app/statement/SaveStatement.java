package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Take the person's statement: record their due-total now (late charges included, at most the sheet's cap) and the sheet's today, bind
 *  each of their claims that counts to its rate figure now (never the floor or its late charge, which keep following
 *  the sheet's floor and today), and unbind the rest (their due is live again; a claim deferred on today is left unbound). A settled claim's
 *  fixed due is left as it is. */
public class SaveStatement {
    public void save(@Val Person loaded, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, Out<Statement> statement, Out<PersonResponse> saved) {
        people.lock(loaded);
        Person person = people.find(loaded.sheetId(), loaded.id());
        var today = sheets.today(person.sheetId());
        var floor = settings.floor(person.sheetId());
        List<Claim> theirs = claims.listForPerson(person.sheetId(), person.id());
        LateCharge late = LateCharge.of(person.sheetId(), settings, sheets, claims);
        BigDecimal total = Dues.capped(Dues.total(person.sheetId(), theirs, person.grades(), person.holds(),
                person.retiredFrom(), today, rates, floor, late), settings.cap(person.sheetId()));
        int counted = 0;
        for (Claim c : theirs) {
            if (Dues.counts(c, person.holds(), person.retiredFrom(), today)) {
                counted++;
                claims.state(c, true, Dues.figure(c, person.grades(), rates, late.rounding()));
            } else if (c.stated()) {
                claims.state(c, false, null);
            }
        }
        people.stated(person, total, today);
        statement.set(new Statement(person.sheetId(), person.name(), total, counted));
        saved.set(PersonResponse.from(people.find(person.sheetId(), person.id())));
    }
}

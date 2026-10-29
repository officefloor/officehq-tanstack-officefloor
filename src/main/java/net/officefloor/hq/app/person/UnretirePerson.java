package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class UnretirePerson {
    // Only a retired person can be unretired. The retirement is kept as a past one, from its date to the sheet's today.
    // Person is a record, so the unretired person is published as the response in place of the loaded one.
    public void unretire(@Val Person person, People people, Sheets sheets, Out<PersonResponse> responseOut)
            throws PersonUnretireRefusedException {
        if (!person.retired()) {
            throw new PersonUnretireRefusedException(person.name() + " is not retired.");
        }
        LocalDate today = sheets.today(person.sheetId());
        Retirement past = new Retirement(person.retiredFrom(), today);
        people.unretire(person.id(), past);
        List<Retirement> retirements = new ArrayList<>(person.retirements());
        retirements.add(past);
        Person unretired = new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                null, person.holds(), person.dated(), person.terms(), person.contact(), person.aliases(), person.origin(),
                List.copyOf(retirements));
        responseOut.set(PersonResponse.from(unretired, today));
    }
}

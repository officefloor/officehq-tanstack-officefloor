package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class RetirePerson {
    // Person is a record, so the retired person is published as the response in place of the loaded one.
    public void retire(@Val RetireFrom from, @Val Person person, People people, Sheets sheets,
            Out<PersonResponse> responseOut) {
        Person retired = new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                from.date() != null ? from.date() : sheets.today(person.sheetId()), person.holds(), person.dated(), person.terms(), person.contact());
        people.retire(retired.id(), retired.retiredFrom());
        responseOut.set(PersonResponse.from(retired, sheets.today(person.sheetId())));
    }
}

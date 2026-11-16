package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the person; their own terms given with a from-date are recorded from it (none before it). */
public class AddPerson {
    public void add(@PathVariable("sheetId") String sheetId, @Val PersonValues values, PersonRepository people,
            Out<Person> person) throws DuplicatePersonNameException, DuplicatePersonContactException {
        String sheet = sheetId.trim();
        if (values.termsFrom() == null) {
            person.set(people.add(sheet, values.name(), values.grade(), values.terms(), values.contact(), values.tags()));
            return;
        }
        Person added = people.add(sheet, values.name(), values.grade(), null, values.contact(), values.tags());
        people.recordTerms(sheet, added.id(), new PersonTerms(values.terms(), values.termsFrom()));
        person.set(people.find(sheet, added.id()));
    }
}

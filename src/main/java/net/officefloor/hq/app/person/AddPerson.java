package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class AddPerson {
    public void add(@PathVariable("sheetId") String sheetId, @Val PersonValues values, PersonRepository people,
            Out<Person> person) throws DuplicatePersonNameException, DuplicatePersonContactException {
        person.set(people.add(sheetId.trim(), values.name(), values.grade(), values.terms(), values.contact(), values.tags()));
    }
}

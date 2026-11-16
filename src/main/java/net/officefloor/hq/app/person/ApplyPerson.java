package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val Person person, @Val PersonValues values, Out<Person> changed) {
        changed.set(new Person(person.id(), person.sheetId(), values.name(), values.grade(), person.version() + 1,
                person.retiredFrom()));
    }
}

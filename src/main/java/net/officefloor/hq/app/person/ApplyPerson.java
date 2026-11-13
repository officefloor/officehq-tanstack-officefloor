package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Applies the edit to the person as loaded; refused when the editor loaded an older version. */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, Out<Person> updated)
            throws StalePersonException {
        if (request.version() != null && request.version() != person.version()) {
            throw new StalePersonException(person.name());
        }
        updated.set(new Person(person.id(), person.sheetId(), request.name(), request.grade(), person.version()));
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Gives the person the alias; one equal to any name or alias on the sheet (case ignored) is a 409. Publishes the person
 *  as stored, with their aliases. */
public class AddAlias {
    public void add(@Val Person person, @Val AliasValues values, PersonRepository people, Out<Person> changed)
            throws DuplicatePersonNameException {
        people.addAlias(person.sheetId(), person.id(), values.alias());
        changed.set(people.find(person.sheetId(), person.id()));
    }
}

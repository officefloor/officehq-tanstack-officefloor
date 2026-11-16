package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron. The person themselves, someone not on the sheet, or a
 *  patron they already have is refused. Publishes the patron. */
public class AddPatron {
    public void add(@Val Person person, @Val PatronAdd add, PersonRepository people, PatronRepository patrons,
            Out<PatronOf> added) throws InvalidPersonException {
        Person patron = people.find(person.sheetId(), add.patronId());
        if (patron == null) {
            throw new InvalidPersonException("No person " + add.patronId());
        }
        if (patron.id() == person.id()) {
            throw new InvalidPersonException("A person cannot be their own patron");
        }
        if (!patrons.add(person.sheetId(), person.id(), patron.id())) {
            throw new InvalidPersonException(patron.name() + " is already a patron of " + person.name());
        }
        added.set(new PatronOf(patron));
    }
}

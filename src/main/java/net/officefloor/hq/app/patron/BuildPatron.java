package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The person's new patron; refused (400) when the patron is not on the sheet, is the person themselves, or is
 * already one of their patrons. Publishes the patron's name for the audit record.
 */
public class BuildPatron {
    public void build(@Val Person person, @Val PatronRequest request, PersonRepository people,
            PatronRepository patrons, Out<Patron> patron, Out<PatronName> patronName) throws InvalidPersonException {
        Person named = people.find(person.sheetId(), request.patronId());
        if (named == null) {
            throw new InvalidPersonException("The patron is not on the sheet.");
        }
        if (named.id().equals(person.id())) {
            throw new InvalidPersonException("A person cannot be their own patron.");
        }
        if (patrons.exists(person.sheetId(), person.id(), named.id())) {
            throw new InvalidPersonException(named.name() + " is already a patron.");
        }
        patron.set(new Patron(person.sheetId(), person.id(), named.id()));
        patronName.set(new PatronName(named.name()));
    }
}

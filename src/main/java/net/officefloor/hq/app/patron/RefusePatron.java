package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Refuse a patron who is not on the sheet, is the person themselves, or is already one of theirs. */
public class RefusePatron {
    public void refuse(@Val PatronRequest request, @Val Person loaded, PersonRepository people,
            PatronRepository patrons, Out<Patron> patron) throws InvalidPatronException {
        Person named = people.find(loaded.sheetId(), request.patronId());
        if (named == null) {
            throw new InvalidPatronException("No person " + request.patronId() + " on this sheet.");
        }
        if (named.id() == loaded.id()) {
            throw new InvalidPatronException(loaded.name() + " cannot be their own patron.");
        }
        people.lock(loaded);
        if (patrons.exists(loaded.sheetId(), loaded.id(), named.id())) {
            throw new InvalidPatronException(named.name() + " is already a patron of " + loaded.name() + ".");
        }
        patron.set(new Patron(loaded.sheetId(), loaded.id(), named.id(), named.name()));
    }
}

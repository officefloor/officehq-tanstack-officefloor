package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

public class LoadPatron {
    // Runs after the person is loaded, so the patron is looked up on the same sheet.
    public void load(@RequestBody PatronRequest body, @Val Person person,
            People people, Patrons patrons, Out<Patron> patronOut) throws PatronRefusedException {
        Person patron = body == null || body.patronId() == null ? null : people.find(person.sheetId(), body.patronId());
        if (patron == null) {
            throw new PatronRefusedException("The patron must be a person on this sheet.");
        }
        if (patron.id() == person.id()) {
            throw new PatronRefusedException("A person cannot be their own patron.");
        }
        if (patrons.has(person.id(), patron.id())) {
            throw new PatronRefusedException(patron.name() + " is already a patron of " + person.name() + ".");
        }
        patronOut.set(new Patron(patron));
    }
}

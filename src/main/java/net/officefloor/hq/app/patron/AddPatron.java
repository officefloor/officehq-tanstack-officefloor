package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Names someone else on the sheet as one of the person's patrons; refused for themselves or a patron already named. */
public class AddPatron {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @RequestBody PatronRequest request, People people, Patrons patrons, Audit audit)
            throws InvalidPatronException {
        Person patron = request == null || request.patron() == null ? null : people.find(sheet, request.patron());
        if (patron == null) {
            throw new InvalidPatronException("A patron must be someone on the sheet");
        }
        if (patron.id() == person.id()) {
            throw new InvalidPatronException("A person cannot be their own patron");
        }
        if (patrons.has(sheet, person.id(), patron.id())) {
            throw new InvalidPatronException(patron.name() + " is already a patron of " + person.name());
        }
        patrons.add(sheet, person.id(), patron.id());
        audit.record("PATRON_ADDED sheet=" + sheet + " by=" + user + " name=" + person.name() + " patron="
                + patron.name());
    }
}

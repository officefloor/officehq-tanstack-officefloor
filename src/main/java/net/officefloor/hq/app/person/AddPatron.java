package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Names the patron for the person: refused (nothing written) when the patron is the person
 *  themselves, is not on the sheet, or is already one of their patrons. */
public class AddPatron {
    public void add(@RequestParam(name = "by", required = false) String by,
            @Val PatronRequest request, @Val Person person, PersonRepository people,
            Out<Patron> added) throws InvalidPersonException {
        long patronId = request.patronId();
        if (patronId == person.getId()) {
            throw new InvalidPersonException("A person cannot be their own patron");
        }
        Person patron = people.find(person.getSheetId(), patronId);
        if (patron == null) {
            throw new InvalidPersonException("No such person on the sheet: " + patronId);
        }
        if (person.getPatronIds().contains(patronId)) {
            throw new InvalidPersonException(patron.getName() + " is already a patron");
        }
        people.addPatron(person, patron, by == null ? "" : by);
        added.set(new Patron(patron));
    }
}

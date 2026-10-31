package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Names another person on the sheet as the person's patron. The person is read afresh and locked,
 *  so two adds at once happen one after the other. Not an edit: the person's version is untouched.
 *  Refuses the person themselves and someone already their patron. Publishes the person as they
 *  are now. */
public class AddPatron {
    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val PatronRequest request, People people, Out<Person> personOut, Out<Patronage> patronageOut)
            throws PersonNotFoundException, InvalidSheetRequestException {
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        Person patron = people.find(sheetId, request.patronId());
        if (patron == null) throw new InvalidSheetRequestException("No such person on this sheet");
        if (patron.getId().equals(person.getId())) {
            throw new InvalidSheetRequestException("A person cannot be their own patron");
        }
        if (person.getPatronIds().contains(patron.getId())) {
            throw new InvalidSheetRequestException(patron.getName() + " is already a patron");
        }
        people.addPatron(person, patron);
        personOut.set(person);
        patronageOut.set(new Patronage(person, patron, request.user()));
    }
}

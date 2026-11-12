package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names the patron as the person's, refusing themselves, someone not on the sheet, or a patron twice. */
public class AddPatron {
    public void add(@Val PatronRequest request, @Val Person person, PersonStore people, PatronStore patrons,
            Out<Patron> added) throws InvalidSheetException {
        Person patron = request.patronId() == null ? null : people.find(person.getSheetId(), request.patronId());
        if (patron == null) {
            throw new InvalidSheetException("No such person to be a patron");
        }
        if (patron.getId() == person.getId()) {
            throw new InvalidSheetException("A person cannot be their own patron");
        }
        if (patrons.of(person.getSheetId(), person.getId()).contains(patron.getId())) {
            throw new InvalidSheetException(patron.getName() + " is already a patron");
        }
        patrons.add(person.getSheetId(), person.getId(), patron.getId());
        added.set(new Patron(patron));
    }
}

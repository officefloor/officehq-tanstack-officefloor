package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron: not themselves, and not one already named. */
public class ApplyPatron {
    public void apply(@Val PatronInput input, @Val Person person, PersonRepository people, PatronRepository patrons,
            Out<PatronAdded> added) throws InvalidPersonException {
        Person patron = people.findByIdAndSheetId(input.patronId(), person.getSheetId())
                .orElseThrow(() -> new InvalidPersonException("No such person on the sheet."));
        if (patron.getId().equals(person.getId())) {
            throw new InvalidPersonException("A person cannot be their own patron.");
        }
        if (patrons.existsByPersonIdAndPatronId(person.getId(), patron.getId())) {
            throw new InvalidPersonException(patron.getName() + " is already a patron.");
        }
        patrons.saveAndFlush(new Patron(person.getSheetId(), person.getId(), patron.getId()));
        added.set(new PatronAdded(patron));
    }
}

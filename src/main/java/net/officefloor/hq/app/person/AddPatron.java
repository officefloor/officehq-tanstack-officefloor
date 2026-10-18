package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron, refusing the person themselves or one already named. */
public class AddPatron {
    public void add(@Val PatronInput input, @Val Person person, PersonRepository repository,
            Out<AddedPatron> addedOut) throws InvalidPersonException {
        Person patron = repository.findByIdAndSheetId(input.patronId(), person.getSheetId())
                .orElseThrow(() -> new InvalidPersonException("No such person on this sheet."));
        if (patron.getId().equals(person.getId())) {
            throw new InvalidPersonException(person.getName() + " cannot be their own patron.");
        }
        if (!person.getPatronIds().add(patron.getId())) {
            throw new InvalidPersonException(patron.getName() + " is already a patron of " + person.getName() + ".");
        }
        addedOut.set(new AddedPatron(patron.getName()));
    }
}

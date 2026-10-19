package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron: not themselves, and not one already named. */
public class AddPatron {
    public void add(@Val PatronRequest request, @Val Person person, PersonRepository repository,
            Out<Patron> patronOut) throws InvalidPersonException {
        Person patron = repository.findById(person.getSheetId(), request.patronId());
        if (patron == null) throw new InvalidPersonException("No such person on the sheet.");
        if (patron.getId() == person.getId()) throw new InvalidPersonException("A person cannot be their own patron.");
        if (repository.isPatron(person, patron.getId())) {
            throw new InvalidPersonException(patron.getName() + " is already a patron.");
        }
        repository.addPatron(person, patron.getId());
        person.getPatrons().add(patron.getName());
        person.getPatrons().sort(null);
        patronOut.set(new Patron(patron.getId(), patron.getName()));
    }
}

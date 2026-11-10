package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron: not themselves, and not twice. */
public class AddPatron {
    public void add(@Val PatronInput input, @Val Sheet sheet, @Val Person person,
            PersonRepository repository, Out<Patron> patronOut) throws InvalidPersonException {
        Person patron = repository.find(sheet.id(), input.patronId());
        if (patron == null) throw new InvalidPersonException("No such patron");
        if (patron.getId().equals(person.getId())) throw new InvalidPersonException("A person cannot be their own patron");
        if (person.getPatronIds().contains(patron.getId())) throw new InvalidPersonException("Already a patron");
        repository.addPatron(person, patron);
        patronOut.set(new Patron(patron));
    }
}

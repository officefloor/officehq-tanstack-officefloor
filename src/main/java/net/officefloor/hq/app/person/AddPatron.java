package net.officefloor.hq.app.person;

import java.util.stream.Stream;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron; refuses the person themselves and a patron they have already. */
public class AddPatron {
    public void add(@Val PatronInput input, @Val Person person, PersonRepository repository, Out<PatronAdded> added)
            throws BadRequestException {
        Person patron = repository.find(person.getSheetId(), input.patronId());
        if (patron == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (patron.getId() == person.getId()) {
            throw new BadRequestException("A person cannot be their own patron");
        }
        if (!repository.addPatron(person.getId(), patron.getId())) {
            throw new BadRequestException(patron.getName() + " is already a patron; reload the sheet");
        }
        person.setPatrons(Stream.concat(person.getPatronsNamed().stream(), Stream.of(patron.getName())).toList());
        added.set(new PatronAdded(patron));
    }
}

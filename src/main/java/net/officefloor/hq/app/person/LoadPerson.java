package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@Val SheetResponse sheet, @PathVariable("personId") String personId,
            PersonRepository repository, Out<Person> person) throws BadRequestException {
        Person found = null;
        try {
            found = repository.find(sheet.id(), Long.parseLong(personId));
        } catch (NumberFormatException e) {
            // not an id: no such person
        }
        if (found == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        person.set(found);
    }
}

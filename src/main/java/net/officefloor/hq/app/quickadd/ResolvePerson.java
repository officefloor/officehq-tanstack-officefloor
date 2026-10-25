package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person on the sheet with the typed name (case and surrounding spaces ignored), else a new one. */
public class ResolvePerson {
    public void resolve(@Val SheetResponse sheet, @Val PersonRequest request, PersonRepository repository,
            Out<Person> person, Out<QuickAddPerson> resolved) {
        Person found = repository.findByName(sheet.id(), request.name());
        boolean added = found == null;
        if (added) {
            found = repository.insert(sheet.id(), request.name(), request.grade());
        }
        person.set(found);
        resolved.set(new QuickAddPerson(found, added));
    }
}

package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person on the sheet with the typed name (ignoring case), or a new, unsaved one with grade A. */
public class FindQuickPerson {
    public void find(@Val QuickAddRequest request, @Val Sheet sheet, PersonRepository repository,
            Out<Person> person) {
        Person found = repository.findByName(sheet.id(), request.name());
        person.set(found != null ? found : new Person(null, sheet.id(), request.name(), "A", 0, 0, null, null));
    }
}

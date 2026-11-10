package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person on the sheet with the typed name (ignoring case), or a new one with grade A, not yet saved. */
public class FindQuickAddPerson {
    public void find(@Val QuickAdd quick, @Val Sheet sheet, PersonRepository repository,
            Out<Person> personOut, Out<QuickAddPerson> createdOut) {
        Person person = repository.findByName(sheet.id(), quick.name());
        createdOut.set(new QuickAddPerson(person == null));
        personOut.set(person != null ? person : new Person(null, sheet.id(), quick.name(), "A"));
    }
}

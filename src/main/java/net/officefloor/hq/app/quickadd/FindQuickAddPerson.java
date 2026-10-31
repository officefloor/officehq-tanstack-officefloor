package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** The person by that name on the sheet, or a new (not yet saved) one when there is none. */
public class FindQuickAddPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, People people,
            Out<Person> person, Out<QuickAddPerson> found) {
        Person existing = people.named(sheetId, request.name());
        found.set(new QuickAddPerson(existing == null));
        person.set(existing != null ? existing : new Person(null, sheetId, request.name(), request.grade()));
    }
}

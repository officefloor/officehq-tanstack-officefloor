package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** The person by that name on the sheet, or a new (not yet saved) one when there is none. Refused:
 *  a new person whose name is used on another sheet of the sheet's tree. */
public class FindQuickAddPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, People people,
            Out<Person> person, Out<QuickAddPerson> found) throws InvalidSheetRequestException {
        Person existing = people.named(sheetId, request.name());
        if (existing == null && people.nameUsedInTree(sheetId, request.name(), null)) {
            throw new InvalidSheetRequestException("The name is already in use on this sheet's tree");
        }
        found.set(new QuickAddPerson(existing == null));
        person.set(existing != null ? existing : new Person(null, sheetId, request.name(), request.grade()));
    }
}

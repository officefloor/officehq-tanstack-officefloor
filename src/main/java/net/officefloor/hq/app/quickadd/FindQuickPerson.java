package net.officefloor.hq.app.quickadd;

import java.util.List;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.NameTree;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person on the sheet with the typed name (ignoring case), or a new, unsaved one with grade A; a new
 * one whose name another sheet of the sheet's tree uses as a name or alias is refused (400). */
public class FindQuickPerson {
    public void find(@Val QuickAddRequest request, @Val Sheet sheet, PersonRepository repository,
            NameTree tree, Out<Person> person) throws InvalidClaimException {
        Person found = repository.findByName(sheet.id(), request.name());
        if (found == null && tree.clashes(sheet.id(), request.name())) {
            throw new InvalidClaimException("The name " + request.name() + " is already in use on a related sheet.");
        }
        person.set(found != null ? found : new Person(null, sheet.id(), request.name(), "A", 0, 0, null, null, null, List.of()));
    }
}

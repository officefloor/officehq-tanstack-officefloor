package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Finds the named person on the sheet (ignoring case; names are trimmed), or builds them, unsaved,
 *  with grade A. */
public class FindQuickPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository, Out<Person> personOut, Out<QuickPerson> quickOut) {
        String sheet = sheetId.trim();
        Person found = repository.findBySheetIdOrderByIdAsc(sheet).stream()
                .filter((p) -> p.getName().equalsIgnoreCase(request.name())).findFirst().orElse(null);
        if (found == null) {
            found = new Person(sheet);
            found.setName(request.name());
            found.setGrade(request.grade());
        }
        quickOut.set(new QuickPerson(found.getId() == null));
        personOut.set(found);
    }
}

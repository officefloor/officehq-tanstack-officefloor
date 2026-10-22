package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Apply the edit to the loaded person and store it, refused when the person has been saved since
 *  the version the edit was made from. */
public class SavePerson {
    public void save(@Val PersonRequest request, @Val Person loaded, PersonRepository repository,
            Out<PersonResponse> saved) throws StalePersonException {
        long from = request.version() == null ? loaded.version() : request.version();
        Person person = new Person(loaded.id(), loaded.sheetId(), request.name(), request.grade(), from + 1,
                loaded.statementCount(), loaded.statedTotal(), loaded.statementDate());
        if (!repository.update(person, from)) {
            throw new StalePersonException(loaded.name());
        }
        saved.set(PersonResponse.from(person));
    }
}

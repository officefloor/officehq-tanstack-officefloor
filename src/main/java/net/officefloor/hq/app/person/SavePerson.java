package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Apply the edit to the loaded person and store it. */
public class SavePerson {
    public void save(@Val PersonRequest request, @Val Person loaded, PersonRepository repository,
            Out<PersonResponse> saved) {
        Person person = new Person(loaded.id(), loaded.sheetId(), request.name(), request.grade());
        repository.update(person);
        saved.set(PersonResponse.from(person));
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SaveImport {
    public void save(@Val ImportedPeople imported, PersonRepository repository) {
        imported.people().forEach(repository::save); // in order, so ids follow the paste
        repository.flush();
    }
}

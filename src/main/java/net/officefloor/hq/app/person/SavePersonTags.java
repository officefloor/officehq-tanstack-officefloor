package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Stores the saved person's tags as the form gave them (blank: cleared), adding each to the sheet's tag list; none given: kept. */
public class SavePersonTags {
    public void save(@Val Person person, @Val PersonRequest request, PersonRepository repository) {
        if (request.tags() != null) {
            repository.saveTags(person.sheetId(), person.id(), request.tagsOrNull());
        }
    }
}

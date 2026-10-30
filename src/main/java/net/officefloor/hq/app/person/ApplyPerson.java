package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, People people)
            throws InvalidPersonException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(person.getId());
        }
        if (people.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidPersonException("The name is already in use");
        }
        person.setName(request.name());
        person.setGrade(request.grade());
    }
}

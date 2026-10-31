package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, People people)
            throws InvalidSheetRequestException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(PersonResponse.from(person));
        }
        if (people.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidSheetRequestException("The name is already in use on this sheet");
        }
        person.setName(request.name());
        person.setGrade(request.grade());
    }
}

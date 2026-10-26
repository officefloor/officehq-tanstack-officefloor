package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    // Person is a record, so the edited person is published in place of the loaded one. The edit is
    // refused when the version its form loaded is not the stored one: someone else saved it since.
    public void apply(@Val PersonRequest request, @Val Person person, People people, Out<PersonResponse> responseOut)
            throws InvalidPersonException, PersonConflictException {
        if (request.version() != null && request.version() != person.version()) {
            throw new PersonConflictException("Someone else has saved this person since it was opened.");
        }
        if (people.nameTaken(person.sheetId(), request.name(), person.id())) {
            throw new InvalidPersonException("The name is already in use.");
        }
        Person edited = person.with(request);
        if (!people.save(edited)) {
            throw new PersonConflictException("Someone else has saved this person since it was opened.");
        }
        responseOut.set(PersonResponse.from(edited));
    }
}

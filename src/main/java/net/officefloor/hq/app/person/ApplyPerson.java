package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    // Person is a record, so the edited person is published in place of the loaded one.
    public void apply(@Val PersonRequest request, @Val Person person, People people, Out<PersonResponse> responseOut)
            throws InvalidPersonException {
        if (people.nameTaken(person.sheetId(), request.name(), person.id())) {
            throw new InvalidPersonException("The name is already in use.");
        }
        Person edited = person.with(request);
        people.save(edited);
        responseOut.set(PersonResponse.from(edited));
    }
}

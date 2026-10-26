package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class HoldPerson {
    // Toggles the hold: a person not held is put on hold, a held one is released. Person is a record, so
    // the toggled person is published as the response in place of the loaded one.
    public void hold(@Val Person person, People people, Out<PersonResponse> responseOut) {
        Person toggled = new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                person.retiredFrom(), !person.held());
        people.hold(toggled.id(), toggled.held());
        responseOut.set(PersonResponse.from(toggled));
    }
}

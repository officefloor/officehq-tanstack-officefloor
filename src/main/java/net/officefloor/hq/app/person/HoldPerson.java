package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Puts the person on hold or takes them off it; holding one already held, or releasing one not held, is refused. */
public class HoldPerson {
    public void hold(@Val Person person, @Val Hold hold, PersonRepository people, Out<Person> held)
            throws InvalidPersonException {
        if (!people.hold(person.sheetId(), person.id(), hold.held())) {
            throw new InvalidPersonException(hold.held() ? "The person is already on hold" : "The person is not on hold");
        }
        held.set(new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                person.retiredFrom(), hold.held()));
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Puts the person on hold, or takes them off it. */
public class ApplyHold {
    public void apply(@Val HoldInput input, @Val Person person, PersonRepository repository) {
        person.setHeld(input.held());
        repository.saveAndFlush(person);
    }
}

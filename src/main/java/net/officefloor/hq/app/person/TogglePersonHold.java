package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Puts the person on hold, or takes them off hold when they are on it. */
public class TogglePersonHold {
    public void toggle(@Val Person person, PersonRepository repository) {
        person.setHeld(!person.isHeld());
        repository.saveHeld(person);
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Puts the person on hold, or takes them off it when they are held. */
public class TogglePersonHold {
    public void toggle(@Val Person person, PersonRepository people) {
        person.setHeld(!person.isHeld());
        people.updateHeld(person);
    }
}

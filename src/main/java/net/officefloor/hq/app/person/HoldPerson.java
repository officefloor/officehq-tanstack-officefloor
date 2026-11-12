package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Puts the person on hold, or takes them off it when they are held. */
public class HoldPerson {
    public void hold(@Val Person person, PersonStore store) {
        store.hold(person, !person.isHeld());
    }
}

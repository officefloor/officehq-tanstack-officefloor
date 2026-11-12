package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Adds the hold to the person's holds. */
public class HoldPerson {
    public void hold(@Val HoldInput input, @Val Person person, PersonStore store) {
        store.hold(person, input.hold());
    }
}

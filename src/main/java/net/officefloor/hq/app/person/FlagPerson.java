package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Puts the flag on the person, replacing any they had. */
public class FlagPerson {
    public void flag(@Val FlagInput input, @Val Person person, PersonStore store) {
        store.flag(person, input.flag());
    }
}

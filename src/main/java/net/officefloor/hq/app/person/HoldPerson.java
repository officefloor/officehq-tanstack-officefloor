package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Puts the person on hold or releases them, refusing one already in that state. */
public class HoldPerson {
    public void hold(@Val HoldInput input, @Val Person person) throws InvalidPersonException {
        if (person.isHeld() == input.held()) {
            throw new InvalidPersonException(person.getName() + (input.held() ? " is already on hold." : " is not on hold."));
        }
        person.setHeld(input.held());
    }
}

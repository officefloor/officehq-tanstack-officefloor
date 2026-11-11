package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses to unretire anyone not retired, before anything is written. */
public class ValidateUnretire {
    public void validate(@Val Person person) throws InvalidPersonException {
        if (!person.isRetired()) throw new InvalidPersonException(person.getName() + " is not retired");
    }
}

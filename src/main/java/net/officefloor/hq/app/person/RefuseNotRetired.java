package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuse to unretire a person who is not retired. */
public class RefuseNotRetired {
    public void refuse(@Val Person loaded) throws InvalidPersonException {
        if (loaded.retiredFrom() == null) {
            throw new InvalidPersonException(loaded.name() + " is not retired.");
        }
    }
}

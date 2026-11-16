package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** An edit must be made from the person's current version; one made from an older version is refused. */
public class CheckPersonVersion {
    public void check(@Val Person person, @Val PersonValues values) throws InvalidPersonException, StalePersonException {
        if (values.version() == null) {
            throw new InvalidPersonException("The version edited is required");
        }
        if (values.version() != person.version()) {
            throw new StalePersonException(person.id());
        }
    }
}

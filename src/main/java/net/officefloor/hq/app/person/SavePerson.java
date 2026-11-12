package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonStore store) throws StalePersonException {
        // Stored only over the version loaded, so a save racing this one cannot be overwritten.
        if (!store.save(person)) {
            throw new StalePersonException();
        }
    }
}

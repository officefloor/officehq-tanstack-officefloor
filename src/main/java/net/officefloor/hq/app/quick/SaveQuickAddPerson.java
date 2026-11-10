package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Adds the quick-add's person when they are new, so the claim has their id; one already on the sheet is left as stored. */
public class SaveQuickAddPerson {
    public void save(@Val QuickAddPerson quick, @Val Person person, PersonRepository repository) {
        if (quick.created()) repository.save(person);
    }
}

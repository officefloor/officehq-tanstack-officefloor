package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Saves the quick-add bar's person when they are new to the sheet. */
public class SaveQuickPerson {
    public void save(@Val QuickPerson quick, @Val Person person, PersonRepository repository) {
        if (quick.added()) {
            repository.saveAndFlush(person); // flushed so the claim is built with the person's id
        }
    }
}

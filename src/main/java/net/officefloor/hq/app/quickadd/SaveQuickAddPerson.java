package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Saves the person only when the quick-add creates them. */
public class SaveQuickAddPerson {
    public void save(@Val Person person, @Val QuickAddPerson found, People people) {
        if (found.created()) people.save(person);
    }
}

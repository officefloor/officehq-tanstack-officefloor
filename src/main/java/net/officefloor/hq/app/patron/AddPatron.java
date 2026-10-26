package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class AddPatron {
    public void add(@Val Person person, @Val Patron patron, Patrons patrons) {
        patrons.add(person.id(), patron.person().id());
    }
}

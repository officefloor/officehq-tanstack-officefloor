package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Flags the person with the reason until the date, replacing any flag they had. */
public class FlagPerson {
    public void flag(@Val Person person, @Val Flag flag, PersonRepository people) {
        people.flag(person.sheetId(), person.id(), flag.reason(), flag.until());
    }
}

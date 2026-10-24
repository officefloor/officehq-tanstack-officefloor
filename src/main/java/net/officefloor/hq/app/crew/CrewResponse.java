package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as the screen shows it: its name, its lead and its members in the order they were added (the screen adds
 *  up their due-totals). */
public record CrewResponse(long id, String name, Person lead, List<Person> members) {

    /** A person of the crew: their id and name. */
    public record Person(long id, String name) {
    }
}

package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew: its name, its lead and its members in the order added. */
public record CrewResponse(String name, Person lead, List<Person> members) {

    /** A person of the crew: their id and name. */
    public record Person(long personId, String name) {
    }
}

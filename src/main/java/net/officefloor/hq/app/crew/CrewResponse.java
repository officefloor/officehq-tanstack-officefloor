package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as the screen reads it: its name, its lead and its members in the order they were added. */
public record CrewResponse(long id, String name, CrewPerson lead, List<CrewPerson> members) {

    public static CrewResponse from(Crew crew) {
        return new CrewResponse(crew.id(), crew.name(), crew.lead(), crew.members());
    }
}

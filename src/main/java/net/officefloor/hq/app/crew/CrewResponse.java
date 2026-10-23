package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as the screen shows it: its lead (id and name) and its members in the order they were added. */
public record CrewResponse(long id, String name, long leadId, String lead, List<Member> members) {

    /** A member: the person and their name. */
    public record Member(long personId, String name) {
    }
}

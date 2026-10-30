package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as stored: its name, its lead and its members in the order added (each a person's id and name). */
public record CrewResponse(String name, String leadId, String leadName, List<Member> members) {

    public record Member(String personId, String name) {
    }
}

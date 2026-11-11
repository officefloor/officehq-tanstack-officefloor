package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew, as the screen reads it: its name, its lead's id and name, and its members in the order added. */
public record CrewResponse(String name, String leadId, String lead, List<CrewMemberResponse> members) {
    public static CrewResponse from(Crew crew, List<CrewMember> members) {
        return new CrewResponse(crew.name(), String.valueOf(crew.leadId()), crew.lead(),
                members.stream().map(CrewMemberResponse::from).toList());
    }
}

package net.officefloor.hq.app.crew;

/** A crew member, as the screen reads it: the person's id and name. */
public record CrewMemberResponse(String personId, String name) {
    public static CrewMemberResponse from(CrewMember m) {
        return new CrewMemberResponse(String.valueOf(m.personId()), m.name());
    }
}

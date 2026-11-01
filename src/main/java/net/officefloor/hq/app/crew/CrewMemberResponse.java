package net.officefloor.hq.app.crew;

/** A crew's member as the screen sees it: the person and their name. */
public record CrewMemberResponse(long personId, String name) {
}

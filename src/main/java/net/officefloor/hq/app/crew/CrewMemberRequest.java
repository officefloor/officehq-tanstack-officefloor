package net.officefloor.hq.app.crew;

/** A crew member as the screen sends it: the crew's name and the person's id. */
public record CrewMemberRequest(String crew, String personId) {
}

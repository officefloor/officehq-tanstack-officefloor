package net.officefloor.hq.app.crew;

/** The body of adding a member to a crew: the crew's name and the person's id. */
public record CrewMemberRequest(String crew, Long personId) {
}

package net.officefloor.hq.app.crew;

/** Body of adding a member to a crew: the crew's name, the person, and who adds it. */
public record CrewMemberRequest(String crew, Long personId, String by) {
}

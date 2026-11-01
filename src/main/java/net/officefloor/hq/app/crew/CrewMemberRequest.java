package net.officefloor.hq.app.crew;

/** Body of a member added to a crew: the crew's name, the person and who adds them. */
public record CrewMemberRequest(String crew, Long personId, String user) {
}

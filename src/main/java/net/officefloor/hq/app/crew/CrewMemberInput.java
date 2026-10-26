package net.officefloor.hq.app.crew;

/** A validated member to add: the crew's name and the person's id. */
public record CrewMemberInput(String crew, long personId) {
}

package net.officefloor.hq.app.crew;

/** A crew member request, validated: the crew's name and the person's id. */
public record CrewMemberInput(String crew, Long personId) {
}

package net.officefloor.hq.app.crew;

/** The member form: the crew (by name) and the person to add (by id). */
public record CrewMemberRequest(String crew, Long personId) {
}

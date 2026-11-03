package net.officefloor.hq.app.crew;

/** A member of a crew: a person, by id, and their name now. */
public record CrewMember(long personId, String name) {
}

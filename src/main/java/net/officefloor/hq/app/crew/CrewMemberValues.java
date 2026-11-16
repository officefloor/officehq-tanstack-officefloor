package net.officefloor.hq.app.crew;

/** A member added: the crew's name, the person (id and name), and who adds it. */
public record CrewMemberValues(String crew, long personId, String name, String by) {
}

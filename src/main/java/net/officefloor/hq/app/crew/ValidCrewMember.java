package net.officefloor.hq.app.crew;

/** A member that passed validation: a person of the sheet neither the crew's lead nor already in it. */
public record ValidCrewMember(String crew, long personId, String name, String user) {
}

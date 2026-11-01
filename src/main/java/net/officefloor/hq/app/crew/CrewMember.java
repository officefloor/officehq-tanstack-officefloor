package net.officefloor.hq.app.crew;

/** A person (personId, `name` theirs) added to a crew, added by `by`. */
public record CrewMember(String sheetId, String crew, long personId, String name, String by) {
}

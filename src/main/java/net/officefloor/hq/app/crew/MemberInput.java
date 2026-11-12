package net.officefloor.hq.app.crew;

/** A member to add, validated: the crew, the person and their name, and the user adding them. */
public record MemberInput(CrewStore.Crew crew, long personId, String personName, String user) {
}

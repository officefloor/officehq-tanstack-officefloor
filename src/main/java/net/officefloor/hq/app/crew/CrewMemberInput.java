package net.officefloor.hq.app.crew;

/** A validated crew member add: who is adding it and the person's id. */
public record CrewMemberInput(String user, long personId) {
}

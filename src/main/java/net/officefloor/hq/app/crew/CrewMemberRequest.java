package net.officefloor.hq.app.crew;

/** Body of a crew member add: who is adding it and the person. */
public record CrewMemberRequest(String user, Long personId) {
}

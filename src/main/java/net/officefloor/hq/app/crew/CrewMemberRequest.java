package net.officefloor.hq.app.crew;

/** The crew to add to, the person joining it (their id), and the signed-in user (`by`). */
public record CrewMemberRequest(String crew, Long person, String by) {
}

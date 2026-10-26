package net.officefloor.hq.app.crew;

/** A crew's name as typed, its lead (a person's id), and the signed-in user (`by`) adding it. */
public record CrewRequest(String name, Long lead, String by) {
}

package net.officefloor.hq.app.crew;

/** Body of adding a crew: its name, its lead (a person's id), and who adds it. */
public record CrewRequest(String name, Long leadId, String by) {
}

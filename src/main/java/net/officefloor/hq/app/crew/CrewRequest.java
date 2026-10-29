package net.officefloor.hq.app.crew;

/** The body of adding a crew, as typed: its name and its lead's id. */
public record CrewRequest(String name, Long leadId) {
}

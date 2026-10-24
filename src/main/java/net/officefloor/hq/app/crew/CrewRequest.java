package net.officefloor.hq.app.crew;

/** The crew entry as submitted: its name and its lead's id. */
public record CrewRequest(String name, Long leadId) {
}

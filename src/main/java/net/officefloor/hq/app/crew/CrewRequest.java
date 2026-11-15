package net.officefloor.hq.app.crew;

/** Body of adding a crew: its name and its lead (a person of the sheet). */
public record CrewRequest(String name, Long leadId) {
}

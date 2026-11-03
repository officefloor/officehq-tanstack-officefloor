package net.officefloor.hq.app.crew;

/** The crew form: a name and its lead (a person on the sheet, by id). */
public record CrewRequest(String name, Long leadId) {
}

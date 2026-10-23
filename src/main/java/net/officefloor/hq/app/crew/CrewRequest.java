package net.officefloor.hq.app.crew;

/** Body of a crew add: who is adding it, the name as typed and the lead (a person's id). */
public record CrewRequest(String user, String name, Long leadId) {
}

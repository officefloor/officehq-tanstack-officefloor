package net.officefloor.hq.app.crew;

/** A crew request, validated: its name, trimmed and not blank, and the lead's person id. */
public record CrewInput(String name, Long leadId) {
}

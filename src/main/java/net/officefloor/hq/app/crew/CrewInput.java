package net.officefloor.hq.app.crew;

/** A crew to add, validated and trimmed: its name, its lead and the user adding it. */
public record CrewInput(String name, long leadId, String leadName, String user) {
}

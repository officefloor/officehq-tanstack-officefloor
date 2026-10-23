package net.officefloor.hq.app.crew;

/** A validated crew add: who is adding it, the name, trimmed, and the lead's person id. */
public record CrewInput(String user, String name, long leadId) {
}

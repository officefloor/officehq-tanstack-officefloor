package net.officefloor.hq.app.crew;

/** A crew as the screen sends it: a name and the lead's person id. */
public record CrewRequest(String name, String leadId) {
}

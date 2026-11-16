package net.officefloor.hq.app.crew;

/** A crew to add once validated: its trimmed name, its lead (id and name, the name set once the lead is found), and who
 *  adds it. */
public record CrewValues(String name, long leadId, String lead, String by) {
}

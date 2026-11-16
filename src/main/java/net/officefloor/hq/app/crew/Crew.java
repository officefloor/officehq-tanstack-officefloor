package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew of the sheet: its name, its lead (personId and name; a null id and a blank name once its lead has left the sheet), and its members in the order added. */
public record Crew(String name, Long leadId, String lead, List<CrewMember> members) {
}

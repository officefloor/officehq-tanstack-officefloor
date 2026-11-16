package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew of the sheet: its name, its lead (personId and name), and its members in the order added. */
public record Crew(String name, long leadId, String lead, List<CrewMember> members) {
}

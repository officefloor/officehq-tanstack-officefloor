package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew on a sheet: its lead and its members (in the order they were added), both people of the sheet. */
public record Crew(long id, String sheetId, String name, long leadId, String lead, List<CrewMember> members) {
}

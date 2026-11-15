package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew on a sheet: its lead and its members (in the order they were added), both people of the sheet.
 *  Its lead is null (named "") once they have left the sheet (transferred away). */
public record Crew(long id, String sheetId, String name, Long leadId, String lead, List<CrewMember> members) {
}

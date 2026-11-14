package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew on a sheet: its name, its lead (null: none, once its lead was transferred away) and its members, in the order they were added. {@code id} is null until saved. */
public record Crew(Long id, String sheetId, String name, CrewPerson lead, List<CrewPerson> members) {
}

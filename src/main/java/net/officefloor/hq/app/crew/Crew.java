package net.officefloor.hq.app.crew;

import java.util.List;

/** One of the sheet's crews: its lead and its members (the lead not among them), in the order added. */
public record Crew(String name, CrewMember lead, List<CrewMember> members) {
}

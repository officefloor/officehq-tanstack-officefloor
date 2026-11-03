package net.officefloor.hq.app.crew;

import java.util.List;

/** A sheet's crew: its name, its lead (by id, and their name now) and its members in the order they were added. */
public record Crew(String name, long leadId, String lead, List<CrewMember> members) {
}

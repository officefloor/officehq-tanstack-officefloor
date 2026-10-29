package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as listed: its name, its lead's id, and its members' ids in the order added. */
public record CrewResponse(String name, long leadId, List<Long> memberIds) {
}

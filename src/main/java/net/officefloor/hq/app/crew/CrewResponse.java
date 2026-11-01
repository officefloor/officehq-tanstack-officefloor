package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as the screen sees it: its name, its lead (the person and their name; null when no longer on
 *  the sheet) and its members in the order added. */
public record CrewResponse(String name, long leadId, String lead, List<CrewMemberResponse> members) {
}

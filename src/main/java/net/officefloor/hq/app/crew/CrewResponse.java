package net.officefloor.hq.app.crew;

import java.util.List;

/** A crew as the front end reads it: its lead (null name when no longer on the sheet) and its members. */
public record CrewResponse(long id, String name, long leadId, String lead, List<MemberResponse> members) {

    /** A member of a crew: the person and their name. */
    public record MemberResponse(long personId, String name) {
    }
}

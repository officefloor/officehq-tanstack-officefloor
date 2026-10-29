package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithCrewMember {
    public void respond(@Val CrewMember member, ObjectResponse<CrewMember> response) {
        response.send(member);
    }
}

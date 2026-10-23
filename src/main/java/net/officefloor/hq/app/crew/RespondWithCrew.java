package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithCrew {
    public void respond(@Val CrewAdded added, ObjectResponse<CrewResponse> response) {
        Crew c = added.crew();
        response.send(new CrewResponse(c.id(), c.name(), c.leadId(), added.lead(), List.of()));
    }
}

package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithCrew {
    public void respond(@Val Crew crew, ObjectResponse<CrewResponse> response) {
        response.send(CrewResponse.from(crew, List.of()));
    }
}

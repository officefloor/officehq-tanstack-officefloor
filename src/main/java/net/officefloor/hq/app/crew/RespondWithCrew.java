package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithCrew {
    public void respond(@Val Crew crew, ObjectResponse<Crew> response) {
        response.send(crew);
    }
}

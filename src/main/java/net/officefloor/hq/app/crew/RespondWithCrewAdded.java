package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithCrewAdded {
    public void respond(@Val CrewInput input, ObjectResponse<AddedResponse> response) {
        response.send(new AddedResponse(input.name()));
    }
}

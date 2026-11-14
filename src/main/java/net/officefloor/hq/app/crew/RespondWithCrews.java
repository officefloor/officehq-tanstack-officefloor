package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's crews as the screen reads them, in the order they were added. */
public class RespondWithCrews {
    public void respond(@Val Crews crews, ObjectResponse<List<CrewResponse>> response) {
        response.send(crews.list().stream().map(CrewResponse::from).toList());
    }
}

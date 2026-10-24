package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the id of the clone made. */
public class RespondWithCloned {

    public record ClonedResponse(String toSheet) {
    }

    public void respond(@Val SheetCloned cloned, ObjectResponse<ClonedResponse> response) {
        response.send(new ClonedResponse(cloned.toSheet()));
    }
}

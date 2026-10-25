package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPaste {
    public void respond(@Val PasteResponse result, ObjectResponse<PasteResponse> response) {
        response.send(result);
    }
}

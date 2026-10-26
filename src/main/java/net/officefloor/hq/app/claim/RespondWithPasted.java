package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPasted {
    public void respond(@Val PasteResponse pasted, ObjectResponse<PasteResponse> response) {
        response.send(pasted);
    }
}

package net.officefloor.hq.app.patron;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPatron {
    public void respond(@Val Patron patron, ObjectResponse<PatronResponse> response) {
        response.send(PatronResponse.from(patron));
    }
}

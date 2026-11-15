package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatch {
    public void respond(@Val Watch watch, ObjectResponse<Watch> response) {
        response.send(watch);
    }
}

package net.officefloor.hq.app.audit;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithActivity {
    public void respond(@Val Activity activity, ObjectResponse<Activity> response) {
        response.send(activity);
    }
}

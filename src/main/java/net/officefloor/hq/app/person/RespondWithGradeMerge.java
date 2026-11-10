package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradeMerge {
    public void respond(@Val GradeMergeResult result, ObjectResponse<GradeMergeResult> response) {
        response.send(result);
    }
}

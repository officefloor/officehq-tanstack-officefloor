package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradeMerge {
    public void respond(@Val GradeMergeResponse result, ObjectResponse<GradeMergeResponse> response) {
        response.send(result);
    }
}

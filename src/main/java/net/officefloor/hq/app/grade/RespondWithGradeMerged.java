package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradeMerged {
    public void respond(@Val GradeMergeResult result, ObjectResponse<GradeMergeResult> response) {
        response.send(result);
    }
}

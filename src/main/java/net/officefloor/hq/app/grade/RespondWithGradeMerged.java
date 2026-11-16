package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradeMerged {
    public void respond(@Val GradeMerged merged, ObjectResponse<GradeMerged> response) {
        response.send(merged);
    }
}

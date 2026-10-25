package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** How many people the grade merge gave the other grade. */
public class RespondWithGradeMerged {
    public void respond(@Val GradeMerged merged, ObjectResponse<GradeMerged> response) {
        response.send(merged);
    }
}

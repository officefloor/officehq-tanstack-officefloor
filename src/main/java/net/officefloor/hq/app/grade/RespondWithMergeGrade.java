package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many people were given the other grade. */
public class RespondWithMergeGrade {
    public void respond(@Val MergeGradeResult result, ObjectResponse<MergeGradeResult> response) {
        response.send(result);
    }
}

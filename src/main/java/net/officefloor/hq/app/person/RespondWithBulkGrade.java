package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected people were changed. */
public class RespondWithBulkGrade {
    public void respond(@Val BulkGradeResult result, ObjectResponse<BulkGradeResult> response) {
        response.send(result);
    }
}

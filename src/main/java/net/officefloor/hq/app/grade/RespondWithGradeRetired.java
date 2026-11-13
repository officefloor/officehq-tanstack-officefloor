package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradeRetired {
    public void respond(@Val GradeRetireInput input, ObjectResponse<GradeResponse> response) {
        response.send(new GradeResponse(input.grade(), true));
    }
}

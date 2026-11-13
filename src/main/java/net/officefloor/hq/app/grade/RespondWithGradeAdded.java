package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradeAdded {
    public void respond(@Val GradeAddInput input, ObjectResponse<GradeResponse> response) {
        response.send(new GradeResponse(input.grade(), false));
    }
}

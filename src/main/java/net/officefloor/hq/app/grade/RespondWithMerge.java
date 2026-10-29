package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithMerge {
    public void respond(@Val MergeResponse merge, ObjectResponse<MergeResponse> response) {
        response.send(merge);
    }
}

package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithMemberAdded {
    public void respond(@Val MemberInput input, ObjectResponse<AddedResponse> response) {
        response.send(new AddedResponse(input.pool().name()));
    }
}

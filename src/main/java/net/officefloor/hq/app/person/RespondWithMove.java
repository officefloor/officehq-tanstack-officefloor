package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithMove {
    public void respond(@Val MoveResult moved, ObjectResponse<MoveResult> response) {
        response.send(moved);
    }
}

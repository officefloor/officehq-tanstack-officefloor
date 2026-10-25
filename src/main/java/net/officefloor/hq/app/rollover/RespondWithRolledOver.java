package net.officefloor.hq.app.rollover;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRolledOver {
    public record Made(int made) {
    }

    public void respond(@Val RolledOver rolledOver, ObjectResponse<Made> response) {
        response.send(new Made(rolledOver.made()));
    }
}

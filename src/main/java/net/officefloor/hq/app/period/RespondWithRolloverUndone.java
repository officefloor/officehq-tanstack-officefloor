package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRolloverUndone {
    /** The fiscal year whose rollover was undone. */
    public record Undone(String fy) {
    }

    public void respond(@Val RolloverRepository.Rollover rollover, ObjectResponse<Undone> response) {
        response.send(new Undone(rollover.fy()));
    }
}

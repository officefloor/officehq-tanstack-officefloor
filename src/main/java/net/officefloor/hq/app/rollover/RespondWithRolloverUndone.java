package net.officefloor.hq.app.rollover;

import net.officefloor.web.ObjectResponse;

public class RespondWithRolloverUndone {
    public record Undone(boolean undone) {
    }

    public void respond(ObjectResponse<Undone> response) {
        response.send(new Undone(true));
    }
}

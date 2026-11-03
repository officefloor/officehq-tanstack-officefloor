package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithNotes {
    public void respond(@Val Notes notes, ObjectResponse<Notes> response) {
        response.send(notes);
    }
}

package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithNote {
    public void respond(@Val Note note, ObjectResponse<Note> response) {
        response.send(note);
    }
}

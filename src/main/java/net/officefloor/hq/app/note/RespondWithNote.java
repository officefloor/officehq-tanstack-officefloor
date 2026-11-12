package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithNote {
    public void respond(@Val NoteResponse note, ObjectResponse<NoteResponse> response) {
        response.send(note);
    }
}

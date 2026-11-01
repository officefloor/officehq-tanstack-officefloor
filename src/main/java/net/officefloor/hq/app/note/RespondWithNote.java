package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithNote {
    public void respond(@Val Note note, ObjectResponse<NoteResponse> response) {
        response.send(NoteResponse.from(note));
    }
}

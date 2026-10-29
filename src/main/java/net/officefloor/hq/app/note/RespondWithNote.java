package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithNote {
    public void respond(@Val Note note, ObjectResponse<NoteResponse> response) {
        response.send(new NoteResponse(0, note.on().toString(), note.by(), note.text()));
    }
}

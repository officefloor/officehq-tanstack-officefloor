package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's notes as the screen reads them, newest first. */
public class RespondWithNotes {
    public void respond(@Val Notes notes, ObjectResponse<List<NoteResponse>> response) {
        response.send(notes.list().stream().map((n) -> new NoteResponse(n.date().toString(), n.user(), n.text())).toList());
    }
}

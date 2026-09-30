package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/notes} — list every note, newest first. The UI scopes them to one target (e.g. a
 * project). Wired by {@code officefloor/rest/api/notes.GET.yml}.
 */
public class NotesGet {

    public void service(NoteRepository notes, ObjectResponse<List<Note>> response) {
        response.send(notes.findAllByOrderByAtDesc());
    }
}

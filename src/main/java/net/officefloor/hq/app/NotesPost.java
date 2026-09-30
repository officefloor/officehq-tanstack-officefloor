package net.officefloor.hq.app;

import java.time.Instant;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/notes — write a note on a target from {targetType, targetId, text} and return the saved
 * row (with its id and the written-at instant). Wired by {@code officefloor/rest/api/notes.POST.yml}.
 * A note must have a target and non-blank text; anything missing is rejected with 400. The
 * written-at instant is stamped server-side (ISO-8601, UTC) so the newest note sorts to the top.
 */
public class NotesPost {

    public void service(@RequestBody NewNote body, NoteRepository notes,
            ObjectResponse<Note> response) {
        String targetType = body.getTargetType();
        Long targetId = body.getTargetId();
        String text = body.getText();
        if (targetType == null || targetType.trim().isEmpty() || targetId == null
                || text == null || text.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Note note = new Note();
        note.setTargetType(targetType.trim());
        note.setTargetId(targetId);
        note.setText(text.trim());
        note.setAt(Instant.now().toString());
        response.send(notes.save(note));
    }
}

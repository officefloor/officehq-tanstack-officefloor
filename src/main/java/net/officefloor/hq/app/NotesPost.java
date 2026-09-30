package net.officefloor.hq.app;

import java.time.Instant;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/notes} — write a note against a target ({@code targetType}/{@code targetId}) and
 * return the created row. The server stamps {@code at} with the current instant so a fresh note is
 * always the newest. Wired by {@code officefloor/rest/api/notes.POST.yml}; the create is audited
 * through {@link Audit}.
 */
public class NotesPost {

    public void service(@RequestBody NewNote body, NoteRepository notes, Audit audit,
            ObjectResponse<Note> response) {
        String text = body.getText() == null ? "" : body.getText().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("a note requires text");
        }
        String targetType = body.getTargetType() == null ? "" : body.getTargetType().trim();
        Long targetId = body.getTargetId();
        if (targetType.isEmpty() || targetId == null) {
            throw new IllegalArgumentException("a note requires a target");
        }
        Note note = new Note();
        note.setTargetType(targetType);
        note.setTargetId(targetId);
        note.setText(text);
        note.setAt(Instant.now().toString());
        Note saved = notes.save(note);
        audit.record("NOTE_CREATED id=" + saved.getId() + " target=" + targetType + ":" + targetId);
        response.send(saved);
    }
}

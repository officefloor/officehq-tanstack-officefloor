package net.officefloor.hq.app;

import java.time.Instant;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/notes} — write a note from a {targetType, targetId, text} body and return the
 * saved row (with its generated id and the instant it was written). Wired by
 * {@code officefloor/rest/api/notes.POST.yml}. The note is stamped with the current instant on the
 * server so it sorts newest-first ahead of every existing note. Target and text are required.
 */
public class NotesPostLogic {

    public void service(@RequestBody NewNote newNote, NoteRepository notes,
            ObjectResponse<Note> response) {
        String targetType = newNote.getTargetType();
        Long targetId = newNote.getTargetId();
        String text = newNote.getText();
        if (targetType == null || targetType.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A note target type is required");
        }
        if (targetId == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A note target id is required");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Note text is required");
        }
        Note saved = notes.save(
                new Note(targetType.trim(), targetId, text.trim(), Instant.now()));
        response.send(saved);
    }

    /** Request body for writing a note. */
    public static class NewNote {
        private String targetType;
        private Long targetId;
        private String text;

        public String getTargetType() {
            return targetType;
        }

        public void setTargetType(String targetType) {
            this.targetType = targetType;
        }

        public Long getTargetId() {
            return targetId;
        }

        public void setTargetId(Long targetId) {
            this.targetId = targetId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}

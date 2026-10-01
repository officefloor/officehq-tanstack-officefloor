package net.officefloor.hq.app;

import java.time.Instant;

/**
 * What the API exposes for a note: the shape the front-end renders into the notes list. {@code at}
 * is the write time as an instant (serialised ISO-8601); the server already returns the rows
 * newest-first, so the UI renders them in order without re-sorting.
 */
public record NoteView(Long id, String targetType, Long targetId, String text, Instant at) {

    public static NoteView of(Note note) {
        return new NoteView(note.getId(), note.getTargetType(), note.getTargetId(), note.getText(),
                note.getCreatedAt());
    }
}

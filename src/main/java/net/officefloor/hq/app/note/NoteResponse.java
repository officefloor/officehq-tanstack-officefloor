package net.officefloor.hq.app.note;

/** A note as the screen sees it: the sheet's today when added (yyyy-mm-dd), who added it, its text. */
public record NoteResponse(String date, String user, String text) {
    static NoteResponse from(Note note) {
        return new NoteResponse(note.addedOn().toString(), note.by(), note.text());
    }
}

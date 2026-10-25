package net.officefloor.hq.app.note;

/** A note as the screen shows it: the sheet's today when it was added (YYYY-MM-DD), who added it, its text. */
public record NoteResponse(long id, String date, String user, String text) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getOnDate().toString(), note.getByUser(), note.getText());
    }
}

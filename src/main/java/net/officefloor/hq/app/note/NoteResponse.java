package net.officefloor.hq.app.note;

/** A note as the screen shows it: the sheet's today when added (ISO yyyy-MM-dd), who added it and its text. */
public record NoteResponse(long id, String on, String by, String text) {
    public static NoteResponse from(Note n) {
        return new NoteResponse(n.id(), n.on().toString(), n.by(), n.text());
    }
}

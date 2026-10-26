package net.officefloor.hq.app.note;

/** A note: the sheet's today when added (yyyy-mm-dd), who added it and its text. */
public record NoteResponse(String date, String by, String text) {

    public static NoteResponse of(Note n) {
        return new NoteResponse(n.addedOn().toString(), n.addedBy(), n.text());
    }
}

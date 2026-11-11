package net.officefloor.hq.app.note;

/** A note, as the screen reads it. */
public record NoteResponse(String addedOn, String addedBy, String text) {
    public static NoteResponse from(Note n) {
        return new NoteResponse(n.addedOn().toString(), n.addedBy(), n.text());
    }
}

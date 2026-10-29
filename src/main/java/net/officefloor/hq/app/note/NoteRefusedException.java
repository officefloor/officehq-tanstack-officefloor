package net.officefloor.hq.app.note;

/** A note that may not be added: blank. */
public class NoteRefusedException extends Exception {

    public NoteRefusedException(String message) {
        super(message);
    }
}

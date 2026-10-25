package net.officefloor.hq.app.note;

/** A note that cannot be added: a blank text. */
public class InvalidNoteException extends Exception {
    public InvalidNoteException(String message) {
        super(message);
    }
}

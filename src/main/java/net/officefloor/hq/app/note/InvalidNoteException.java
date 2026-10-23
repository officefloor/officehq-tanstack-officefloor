package net.officefloor.hq.app.note;

/** A note add with no signed-in user or a blank text. */
public class InvalidNoteException extends Exception {
    public InvalidNoteException(String message) {
        super(message);
    }
}

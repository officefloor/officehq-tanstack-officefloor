package net.officefloor.hq.app.note;

/** A note that cannot be added (blank); nothing is written. */
public class InvalidNoteException extends Exception {
    public InvalidNoteException(String message) {
        super(message);
    }
}

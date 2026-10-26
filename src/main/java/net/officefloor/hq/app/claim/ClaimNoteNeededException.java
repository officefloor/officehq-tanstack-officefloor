package net.officefloor.hq.app.claim;

/** A claim whose due is above the note threshold in force on its date was opened with an empty note. */
public class ClaimNoteNeededException extends Exception {

    public ClaimNoteNeededException(String message) {
        super(message);
    }
}

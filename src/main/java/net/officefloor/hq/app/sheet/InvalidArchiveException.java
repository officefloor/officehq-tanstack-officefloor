package net.officefloor.hq.app.sheet;

/** The sheet cannot be archived: the user is not a steward, or it is archived already. */
public class InvalidArchiveException extends Exception {
    public InvalidArchiveException(String message) {
        super(message);
    }
}

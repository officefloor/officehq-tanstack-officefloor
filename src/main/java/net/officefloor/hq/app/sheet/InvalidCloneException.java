package net.officefloor.hq.app.sheet;

/** The sheet cannot be cloned: the user is not a steward, or the other sheet is not empty. */
public class InvalidCloneException extends Exception {
    public InvalidCloneException(String message) {
        super(message);
    }
}

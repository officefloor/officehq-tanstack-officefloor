package net.officefloor.hq.app.sheet;

/** A clone refused: the user is not a steward, or the id to clone to has been used. */
public class InvalidCloneException extends Exception {
    public InvalidCloneException(String message) {
        super(message);
    }
}

package net.officefloor.hq.app.sheet;

/** A clone refused: the other sheet is already used, or none is named. Nothing is written. */
public class InvalidCloneException extends Exception {
    public InvalidCloneException(String message) {
        super(message);
    }
}

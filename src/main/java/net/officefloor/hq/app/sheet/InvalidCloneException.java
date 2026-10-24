package net.officefloor.hq.app.sheet;

/** A clone of the sheet that is refused: no id given, one already used, or not by a steward. */
public class InvalidCloneException extends Exception {
    public InvalidCloneException(String message) {
        super(message);
    }
}

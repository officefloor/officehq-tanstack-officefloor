package net.officefloor.hq.app.columns;

/** The column chosen is not one of the people list's choosable columns. */
public class InvalidColumnException extends Exception {
    public InvalidColumnException(String message) {
        super(message);
    }
}

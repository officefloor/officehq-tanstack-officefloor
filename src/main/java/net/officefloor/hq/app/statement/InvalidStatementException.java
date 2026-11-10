package net.officefloor.hq.app.statement;

/** A statement that cannot be taken as of the date given. */
public class InvalidStatementException extends Exception {
    public InvalidStatementException(String message) {
        super(message);
    }
}

package net.officefloor.hq.app.statement;

/** A statement cancelled for a person who has none. */
public class NoStatementException extends Exception {
    public NoStatementException(String message) {
        super(message);
    }
}

package net.officefloor.hq.app.statement;

/** A statement may not be cancelled: the user is not a steward, or the person has no statement. */
public class StatementRefusedException extends Exception {

    public StatementRefusedException(String message) {
        super(message);
    }
}

package net.officefloor.hq.app.period;

/** A period action was refused: no such period on the sheet, it is already closed, or (on reopening) it is not closed or the user is not a steward. */
public class PeriodRefusedException extends Exception {

    public PeriodRefusedException(String message) {
        super(message);
    }
}

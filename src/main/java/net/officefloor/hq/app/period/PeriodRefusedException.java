package net.officefloor.hq.app.period;

/** A period action was refused: no such period on the sheet, or it is already closed. */
public class PeriodRefusedException extends Exception {

    public PeriodRefusedException(String message) {
        super(message);
    }
}

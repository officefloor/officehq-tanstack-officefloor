package net.officefloor.hq.app.period;

/** A period was refused: a blank or taken name, a bad date, a to-date before its from-date, or an overlap with another period. */
public class InvalidPeriodException extends Exception {

    public InvalidPeriodException(String message) {
        super(message);
    }
}

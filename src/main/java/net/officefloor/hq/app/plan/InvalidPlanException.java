package net.officefloor.hq.app.plan;

/** The plan's amount is one the claim form refuses (or negative), a date is not a date, its interval is
 *  not a whole number of at least 1, its day of the month not 1 to 31 (or both or neither given), or its end is before its start: refused and nothing is written. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }
}

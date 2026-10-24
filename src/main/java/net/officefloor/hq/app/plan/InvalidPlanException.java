package net.officefloor.hq.app.plan;

/** A plan that cannot be added: a bad amount, date or interval, or an end before its start. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }
}
